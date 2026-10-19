package com.broll.poc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.broll.poc.data.IndexDatabase
import com.broll.poc.index.VideoIndexer
import com.broll.poc.metrics.MetricsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class IndexForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var indexJob: Job? = null
    @Volatile
    private var foregroundStarted = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(runCatching { LocaleStore.wrap(newBase) }.getOrDefault(newBase))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                indexJob?.cancel()
                teardownForeground()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_INDEX -> {
                if (indexJob?.isActive == true) return START_NOT_STICKY
                IndexTaskCallbacks.running = true
                IndexTaskCallbacks.lastProgress = null
                IndexTaskCallbacks.lastResult = null
                val uris = intent.getStringArrayListExtra(EXTRA_URIS)
                val names = intent.getStringArrayListExtra(EXTRA_NAMES)
                if (uris == null || uris.isEmpty()) {
                    IndexTaskCallbacks.finish(IndexResult.Error("缺少视频 URI"))
                    stopSelf()
                    return START_NOT_STICKY
                }
                val videoNames = names ?: ArrayList(uris.map { "video" })
                ensureChannel()
                val notification = buildNotification(getString(R.string.index_notification_start))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                foregroundStarted = true
                acquireWakeLock()
                indexJob?.cancel()
                val app = application as BrollApplication
                indexJob = app.applicationScope.launch {
                    val logger = MetricsLogger(this@IndexForegroundService)
                    EngineHolder.bindMetrics(logger)
                    val summaries = ArrayList<VideoIndexer.Summary>()
                    try {
                        IndexTaskCallbacks.status(getString(R.string.index_engine_init))
                        val engine = EngineHolder.open()
                        for (i in uris.indices) {
                            val uri = Uri.parse(uris[i])
                            val name = videoNames.getOrNull(i) ?: uri.lastPathSegment ?: "video"
                            val (database, dbFailure) = IndexDatabase.tryOpen(this@IndexForegroundService)
                            if (dbFailure != null) {
                                IndexTaskCallbacks.finish(IndexResult.Error(dbFailure.message))
                                return@launch
                            }
                            val db = database ?: run {
                                IndexTaskCallbacks.finish(IndexResult.Error("索引库打开失败"))
                                return@launch
                            }
                            val indexer = VideoIndexer(this@IndexForegroundService, db, logger)
                            try {
                                val summary = indexer.index(
                                    uri = uri,
                                    videoName = name,
                                    embedder = engine,
                                    batchIndex = i + 1,
                                    batchTotal = uris.size,
                                )
                                summaries.add(summary)
                            } finally {
                                db.close()
                            }
                        }
                        IndexTaskCallbacks.finish(IndexResult.Success(summaries))
                    } catch (e: CancellationException) {
                        logger.log(event = "index", stage = "cancelled", detail = "用户取消")
                        IndexTaskCallbacks.finish(IndexResult.Cancelled)
                    } catch (e: Throwable) {
                        logger.log(event = "index", stage = "error", detail = "${e.javaClass.simpleName}: ${e.message}")
                        IndexTaskCallbacks.finish(IndexResult.Error(e.message ?: e.toString()))
                    } finally {
                        releaseWakeLock()
                        teardownForeground()
                        stopSelf()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        indexJob?.cancel()
        releaseWakeLock()
        teardownForeground()
        // Cancellation may still be unwinding native inference. finish() releases the UI gate.
        if (indexJob == null) IndexTaskCallbacks.running = false
        super.onDestroy()
    }

    private fun teardownForeground() {
        if (foregroundStarted) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Throwable) {
            }
            foregroundStarted = false
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jingku:index").apply {
            setReferenceCounted(false)
            acquire(6 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.index_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.index_channel_desc)
            },
        )
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val cancel = PendingIntent.getService(this, 1,
            Intent(this, IndexForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification, getString(R.string.btn_cancel), cancel)
            .build()
    }

    companion object {
        const val ACTION_INDEX = "com.broll.poc.action.INDEX"
        const val ACTION_STOP = "com.broll.poc.action.STOP_INDEX"
        const val EXTRA_URIS = "uris"
        const val EXTRA_NAMES = "names"
        private const val CHANNEL_ID = "jingku_index"
        private const val NOTIFICATION_ID = 42

        fun startIndexBatch(context: Context, items: List<Pair<Uri, String>>) {
            val uris = ArrayList(items.map { it.first.toString() })
            val names = ArrayList(items.map { it.second })
            val intent = Intent(context, IndexForegroundService::class.java).apply {
                action = ACTION_INDEX
                putStringArrayListExtra(EXTRA_URIS, uris)
                putStringArrayListExtra(EXTRA_NAMES, names)
            }
            context.startForegroundService(intent)
        }

        fun stopIndex(context: Context) {
            context.startService(
                Intent(context, IndexForegroundService::class.java).apply { action = ACTION_STOP },
            )
        }
    }
}

sealed class IndexResult {
    data class Success(val summaries: List<VideoIndexer.Summary>) : IndexResult()
    data class Error(val message: String) : IndexResult()
    data object Cancelled : IndexResult()
}

object IndexTaskCallbacks {
    @Volatile var running = false
    @Volatile var lastProgress: IndexProgress? = null
    @Volatile var lastResult: IndexResult? = null
    @Volatile var owner: Any? = null
    @Volatile var onStatus: (String) -> Unit = {}
    @Volatile var onProgress: (IndexProgress) -> Unit = {}
    @Volatile var onFinished: (IndexResult) -> Unit = {}
    fun status(message: String) { onStatus(message) }
    fun progress(value: IndexProgress) { lastProgress = value; onProgress(value) }
    fun finish(result: IndexResult) { running = false; lastResult = result; onFinished(result) }
    fun detach(activity: Any) {
        if (owner === activity) { owner = null; onStatus = {}; onProgress = {}; onFinished = {} }
    }
}
