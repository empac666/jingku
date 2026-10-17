package com.broll.poc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import android.provider.OpenableColumns
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.broll.poc.data.IndexDatabase
import com.broll.poc.data.Segment
import com.broll.poc.embed.truncateNormalize
import com.broll.poc.metrics.MetricsLogger
import com.broll.poc.model.AsrModelStore
import com.broll.poc.model.ModelStore
import com.broll.poc.ui.SearchResultBinder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null
    private var indexing = false

    private var db: IndexDatabase? = null
    private lateinit var logger: MetricsLogger
    private lateinit var store: ModelStore
    private lateinit var asrStore: AsrModelStore

    private lateinit var textStatus: TextView
    private lateinit var textEmbedStatus: TextView
    private lateinit var textAsrStatus: TextView
    private lateinit var textImportList: TextView
    private lateinit var textIndexDetail: TextView
    private lateinit var editQuery: TextInputEditText
    private lateinit var resultContainer: ViewGroup
    private lateinit var videoView: VideoView
    private lateinit var progressEmbed: LinearProgressIndicator
    private lateinit var progressAsr: LinearProgressIndicator
    private lateinit var progressIndex: LinearProgressIndicator

    private val importQueue = ArrayList<Pair<Uri, String>>()
    private var playingUri: Uri? = null
    private var preparedPlayer: android.media.MediaPlayer? = null
    private var lastSearchQuery: String? = null
    private var pendingSeekMs = 0L
    private val thumbJobs = mutableMapOf<View, Job>()

    private val pickVisualMultiple = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris ->
        if (uris.isNotEmpty()) addVideos(uris, "gallery")
    }

    private val pickSafMultiple = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
        }
        addVideos(uris, "saf")
    }

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        startIndexInternal()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(runCatching { LocaleStore.wrap(newBase) }.getOrDefault(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            (application as BrollApplication).startupFailure?.let { throw it }
            initializeUi(savedInstanceState)
        } catch (error: Throwable) {
            showStartupFailure(error)
        }
        showPreviousCrash()
    }

    private fun initializeUi(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        textStatus = findViewById(R.id.textStatus)
        textEmbedStatus = findViewById(R.id.textEmbedStatus)
        textAsrStatus = findViewById(R.id.textAsrStatus)
        textImportList = findViewById(R.id.textImportList)
        textIndexDetail = findViewById(R.id.textIndexDetail)
        editQuery = findViewById(R.id.editQuery)
        resultContainer = findViewById(R.id.resultContainer)
        videoView = findViewById(R.id.videoView)
        progressEmbed = findViewById(R.id.progressEmbed)
        progressAsr = findViewById(R.id.progressAsr)
        progressIndex = findViewById(R.id.progressIndex)

        logger = MetricsLogger(this)
        EngineHolder.bindMetrics(logger)
        store = ModelStore(this)
        asrStore = AsrModelStore(this)
        store.ensureDirs()
        asrStore.ensureDirs()

        wireIndexCallbacks()
        indexing = IndexTaskCallbacks.running
        progressIndex.isVisible = indexing
        IndexTaskCallbacks.lastProgress?.let { if (indexing) IndexTaskCallbacks.onProgress(it) }
        if (savedInstanceState != null) {
            val savedUris = savedInstanceState.getStringArrayList("import_uris").orEmpty()
            val savedNames = savedInstanceState.getStringArrayList("import_names").orEmpty()
            savedUris.forEachIndexed { i, uri -> importQueue.add(Uri.parse(uri) to (savedNames.getOrNull(i) ?: "video")) }
            editQuery.setText(savedInstanceState.getString("query", ""))
        }
        videoView.setMediaController(MediaController(this))
        videoView.setOnErrorListener { _, _, _ ->
            playingUri = null; preparedPlayer = null
            setStatus(getString(R.string.play_error), true); true
        }
        toolbar.setOnLongClickListener { showLicenses(); true }

        findViewById<MaterialButton>(R.id.btnLanguage).setOnClickListener { toggleLanguage() }
        findViewById<MaterialButton>(R.id.btnDownloadEmbed).setOnClickListener { downloadEmbed() }
        findViewById<MaterialButton>(R.id.btnDownloadAsr).setOnClickListener { downloadAsr() }
        findViewById<MaterialButton>(R.id.btnDownloadVad).setOnClickListener { downloadVad() }
        findViewById<MaterialButton>(R.id.btnPickGallery).setOnClickListener {
            pickVisualMultiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
        findViewById<MaterialButton>(R.id.btnPickSaf).setOnClickListener {
            pickSafMultiple.launch(arrayOf("video/*"))
        }
        findViewById<MaterialButton>(R.id.btnStartIndex).setOnClickListener { startIndex() }
        findViewById<MaterialButton>(R.id.btnCancelIndex).setOnClickListener {
            if (indexing) {
                IndexForegroundService.stopIndex(this)
            } else {
                work?.cancel()
            }
        }
        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener { startSearch() }

        refreshModelStatus()
        refreshImportSummary()
        scope.launch {
            var pending: IndexDatabase? = null
            try {
                val (opened, failure) = withContext(Dispatchers.IO) {
                    IndexDatabase.tryOpen(applicationContext).also { pending = it.first }
                }
                db = opened
                pending = null
                if (failure != null) showIndexRebuildDialog(failure.message)
                val count = withContext(Dispatchers.IO) { opened?.countDialogue() }
                setStatus(if (count != null) getString(R.string.status_dialogue_count, count.toInt())
                    else getString(R.string.status_db_unavailable), count == null)
                logger.log(event = "app", stage = "start", detail = "dialogue=$count")
            } catch (error: Throwable) {
                runCatching { pending?.close() }
                if (error is CancellationException) throw error
                showStartupFailure(error)
            }
        }
        updateLanguageButtonLabel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::editQuery.isInitialized) {
            outState.putStringArrayList("import_uris", ArrayList(importQueue.map { it.first.toString() }))
            outState.putStringArrayList("import_names", ArrayList(importQueue.map { it.second }))
            outState.putString("query", editQuery.text?.toString())
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        IndexTaskCallbacks.detach(this)
        if (::videoView.isInitialized) videoView.stopPlayback()
        scope.cancel()
        thumbJobs.values.forEach { it.cancel() }
        thumbJobs.clear()
        work?.cancel()
        runCatching { db?.close() }
        super.onDestroy()
    }

    private fun showStartupFailure(error: Throwable) {
        IndexTaskCallbacks.detach(this)
        scope.cancel()
        val trace = CrashGuard.describe(error)
        val message = getString(R.string.startup_failed) + "\n\n" + trace
        val content = android.widget.TextView(this).apply { text = message; setPadding(24, 24, 24, 24) }
        setContentView(android.widget.ScrollView(this).apply { addView(content) })
        showReport(getString(R.string.startup_error_title), message)
    }

    private fun showPreviousCrash() {
        val file = java.io.File(filesDir, "crash/last_crash.txt")
        val trace = runCatching { if (file.isFile) file.readText() else null }.getOrNull() ?: return
        showReport(getString(R.string.crash_report_title), trace) { file.delete() }
    }

    private fun showReport(title: String, trace: String, dismiss: (() -> Unit)? = null) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title).setMessage(trace).setCancelable(false)
            .setPositiveButton(R.string.crash_copy) { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(title, trace))
            }
            .setNeutralButton(R.string.crash_share) { _, _ ->
                runCatching {
                    startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, trace), title))
                }.onFailure { toast(getString(R.string.crash_share_unavailable)) }
            }
            .setNegativeButton(R.string.crash_dismiss) { _, _ -> dismiss?.invoke() }
            .show()
    }

    private fun toggleLanguage() {
        val next = if (LocaleStore.getLanguage(this) == LocaleStore.LANG_ZH) {
            LocaleStore.LANG_EN
        } else {
            LocaleStore.LANG_ZH
        }
        LocaleStore.setLanguage(this, next)
        recreate()
    }

    private fun updateLanguageButtonLabel() {
        val btn = findViewById<MaterialButton>(R.id.btnLanguage)
        btn.text = if (LocaleStore.getLanguage(this) == LocaleStore.LANG_ZH) {
            getString(R.string.lang_switch)
        } else {
            getString(R.string.lang_switch_zh)
        }
    }

    private fun wireIndexCallbacks() {
        IndexTaskCallbacks.owner = this
        IndexTaskCallbacks.onStatus = { msg ->
            runOnUiThread { textIndexDetail.text = msg; textIndexDetail.isVisible = indexing }
        }
        IndexTaskCallbacks.onProgress = { progress ->
            runOnUiThread {
                indexing = true
                progressIndex.isVisible = true
                val batchLine = getString(
                    R.string.index_progress_batch,
                    progress.batchIndex,
                    progress.batchTotal,
                    progress.videoName,
                )
                textIndexDetail.text = "$batchLine\n${progress.message}\n" + getString(R.string.index_overall, progress.batchIndex - 1, progress.batchTotal)
                textIndexDetail.isVisible = true
                val pct = progress.embedPercent
                if (pct != null) {
                    progressIndex.isIndeterminate = false
                    progressIndex.max = 100
                    progressIndex.setProgressCompat(pct, true)
                } else {
                    progressIndex.isIndeterminate = true
                }
            }
        }
        IndexTaskCallbacks.onFinished = { result ->
            runOnUiThread {
                indexing = false
                progressIndex.isVisible = false
                textIndexDetail.isVisible = false
                when (result) {
                    is IndexResult.Success -> {
                        val totalSeg = result.summaries.sumOf { it.dialogueSegments }
                        setStatus(getString(R.string.index_done, totalSeg))
                        db?.let { setStatus(getString(R.string.status_dialogue_count, it.countDialogue())) }
                    }
                    is IndexResult.Cancelled -> setStatus(getString(R.string.index_cancelled))
                    is IndexResult.Error -> setStatus(result.message, error = true)
                }
                refreshModelStatus()
            }
        }
    }

    private fun addVideos(uris: List<Uri>, source: String) {
        for (uri in uris) {
            try { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            val name = runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: uri.lastPathSegment ?: "video"
            if (importQueue.none { it.first == uri }) {
                importQueue.add(uri to name)
            }
            logger.log(event = "video", stage = "picked", detail = "source=$source uri=$uri")
        }
        refreshImportSummary()
        setStatus(getString(R.string.import_queue_count, importQueue.size))
    }

    private fun refreshImportSummary() {
        textImportList.text = if (importQueue.isEmpty()) {
            getString(R.string.import_none)
        } else {
            importQueue.mapIndexed { i, pair -> "${i + 1}. ${pair.second}" }.joinToString("\n")
        }
    }

    private fun downloadEmbed() {
        if (!beginWork()) return
        progressEmbed.isVisible = true
        progressEmbed.isIndeterminate = true
        work = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    store.download { read, total ->
                        runOnUiThread {
                            showDownloadProgress(progressEmbed, getString(R.string.model_embed), read, total)
                        }
                    }
                }
                withContext(Dispatchers.Default) { EngineHolder.reset() }
                refreshModelStatus()
                setStatus(getString(R.string.model_ready))
            } catch (e: CancellationException) {
                setStatus(getString(R.string.index_cancelled))
            } catch (e: Throwable) {
                setStatus(e.message ?: e.toString(), error = true)
            } finally {
                progressEmbed.isVisible = false
                endWork()
            }
        }
    }

    private fun downloadAsr() {
        if (!beginWork()) return
        progressAsr.isVisible = true
        progressAsr.isIndeterminate = true
        work = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    asrStore.downloadSenseVoiceArchive { read, total ->
                        runOnUiThread {
                            showDownloadProgress(progressAsr, getString(R.string.model_asr), read, total)
                        }
                    }
                }
                refreshModelStatus()
                setStatus(getString(R.string.model_ready))
            } catch (e: Throwable) {
                setStatus(e.message ?: e.toString(), error = true)
            } finally {
                progressAsr.isVisible = false
                endWork()
            }
        }
    }

    private fun downloadVad() {
        if (!beginWork()) return
        progressAsr.isVisible = true
        progressAsr.isIndeterminate = true
        work = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    asrStore.downloadVad { read, total ->
                        runOnUiThread {
                            showDownloadProgress(progressAsr, getString(R.string.model_vad), read, total)
                        }
                    }
                }
                refreshModelStatus()
                setStatus(getString(R.string.model_ready))
            } catch (e: Throwable) {
                setStatus(e.message ?: e.toString(), error = true)
            } finally {
                progressAsr.isVisible = false
                endWork()
            }
        }
    }

    private fun showDownloadProgress(bar: LinearProgressIndicator, label: String, read: Long, total: Long) {
        if (total > 0) {
            bar.isIndeterminate = false
            bar.max = 100
            bar.setProgressCompat(((read * 100) / total).toInt().coerceIn(0, 100), true)
        } else {
            bar.isIndeterminate = true
        }
        textStatus.text = getString(
            R.string.download_progress,
            label,
            fmtMb(read),
            if (total > 0) fmtMb(total) else "?",
        )
    }

    private fun startIndex() {
        if (importQueue.isEmpty()) {
            toast(getString(R.string.index_need_video))
            return
        }
        if (!asrStore.isSenseVoiceReady()) {
            toast(getString(R.string.index_need_asr))
            return
        }
        if (store.locate() == null) {
            toast(getString(R.string.index_need_embed))
            return
        }
        if (indexing || IndexTaskCallbacks.running || work?.isActive == true) {
            toast(getString(R.string.index_busy))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startIndexInternal()
    }

    private fun startIndexInternal() {
        indexing = true
        progressIndex.isVisible = true
        progressIndex.isIndeterminate = true
        try { IndexForegroundService.startIndexBatch(this, importQueue.toList()) }
        catch (e: Exception) { indexing = false; progressIndex.isVisible = false; setStatus(e.message ?: "Index failed", true) }
    }

    private fun openIndexDatabaseOrPrompt() {
        val (opened, failure) = IndexDatabase.tryOpen(this)
        if (failure != null) {
            db = null
            showIndexRebuildDialog(failure.message)
            return
        }
        db = opened
    }

    private fun showIndexRebuildDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.db_dialog_title))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.db_dialog_rebuild)) { _, _ ->
                val (opened, wipeFailure) = IndexDatabase.wipeAndOpen(this)
                if (wipeFailure != null) {
                    db = null
                    showIndexRebuildDialog(wipeFailure.message)
                    return@setPositiveButton
                }
                db = opened
                setStatus(getString(R.string.db_rebuild_hint))
                toast(getString(R.string.db_rebuilt))
            }
            .setNegativeButton(getString(R.string.db_dialog_later)) { _, _ ->
                setStatus(message, error = true)
            }
            .show()
    }

    private fun requireDb(): IndexDatabase {
        val index = db
        if (index == null) {
            toast(getString(R.string.status_db_unavailable))
            error("index db not open")
        }
        return index
    }

    private fun startSearch() {
        val raw = editQuery.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) return
        if (db == null) {
            openIndexDatabaseOrPrompt()
            return
        }
        if (!beginWork()) return
        lastSearchQuery = raw
        work = scope.launch {
            try {
                val t0 = System.nanoTime()
                val engine = withContext(Dispatchers.Default) { EngineHolder.open() }
                val appContext = applicationContext
                val hits = withContext(Dispatchers.Default) {
                    val (searchDb, failure) = IndexDatabase.tryOpen(appContext)
                    val indexDb = searchDb ?: error(failure?.message ?: "Index unavailable")
                    try {
                        indexDb.requireSearchModeCompatible(engine.mode)
                        val prefixed = AppConfig.QUERY_PREFIX + raw
                        val vector = engine.embedText(prefixed)
                        val query768 = truncateNormalize(vector, AppConfig.FULL_DIM)
                        indexDb.searchSemantic768(query768, AppConfig.SEARCH_TOP_K)
                    } finally { indexDb.close() }
                }
                val ms = (System.nanoTime() - t0) / 1_000_000
                logger.log(event = "search", stage = "semantic768", elapsedMs = ms, detail = "hits=${hits.size}")
                showHits(hits, raw)
                setStatus(getString(R.string.search_done, hits.size, ms))
            } catch (e: CancellationException) {
                setStatus(getString(R.string.index_cancelled))
            } catch (e: Throwable) {
                setStatus(e.message ?: e.toString(), error = true)
            } finally {
                endWork()
            }
        }
    }

    private fun showHits(hits: List<Segment>, query: String) {
        thumbJobs.values.forEach { it.cancel() }
        thumbJobs.clear()
        resultContainer.removeAllViews()
        if (hits.isEmpty()) {
            val tv = TextView(this)
            tv.text = getString(R.string.search_empty)
            resultContainer.addView(tv)
            return
        }
        for (hit in hits) {
            val card = SearchResultBinder.inflateCard(resultContainer)
            SearchResultBinder.bind(card, hit, query, scope, { play(it) }, thumbJobs)
            resultContainer.addView(card)
        }
    }

    private fun play(seg: Segment) {
        val uri = Uri.parse(seg.videoUri)
        val seekSec = seg.seekSecForQuery(lastSearchQuery)
        val ms = (seekSec * 1000.0).toLong().coerceAtLeast(0L)
        pendingSeekMs = ms
        logger.log(
            event = "play",
            stage = "seek",
            detail = "seek=$seekSec segment=${seg.startSec} score=${seg.score}",
            embedderMode = seg.embedderMode,
        )
        if (playingUri != uri) {
            playingUri = uri
            preparedPlayer = null
            videoView.setOnPreparedListener { player ->
                preparedPlayer = player
                player.setOnSeekCompleteListener { it.start() }
                player.seekTo(pendingSeekMs, android.media.MediaPlayer.SEEK_CLOSEST)
            }
            videoView.setVideoURI(uri)
        } else {
            val player = preparedPlayer
            if (player != null) {
                player.setOnSeekCompleteListener { it.start() }
                player.seekTo(pendingSeekMs, android.media.MediaPlayer.SEEK_CLOSEST)
            }
        }
        val note = if (seg.seekAlignmentNote(lastSearchQuery).isBlank()) ""
            else getString(R.string.segment_start_unaligned)
        setStatus(getString(R.string.play_seek, seekSec) + if (note.isBlank()) "" else " · $note")
        videoView.post { videoView.parent.requestChildFocus(videoView, videoView) }
    }

    private fun showLicenses() {
        val text = assets.open("licenses/LICENSING.md").bufferedReader().use { it.readText() }
        AlertDialog.Builder(this).setTitle(getString(R.string.about_licenses))
            .setMessage(text).setPositiveButton(android.R.string.ok, null).show()
    }

    private fun refreshModelStatus() {
        scope.launch {
            try {
                val (embedReady, asrReady, vadReady) = withContext(Dispatchers.IO) {
                    Triple(store.locate() != null, asrStore.isSenseVoiceReady(), asrStore.isVadReady())
                }
                textEmbedStatus.text = "${getString(R.string.model_embed)}: " +
                    if (embedReady) getString(R.string.model_ready) else getString(R.string.model_missing)
                textAsrStatus.text = buildString {
                    append(getString(R.string.model_asr))
                    append(": ")
                    append(if (asrReady) getString(R.string.model_ready) else getString(R.string.model_missing))
                    append(" · ")
                    append(getString(R.string.model_vad))
                    append(": ")
                    append(if (vadReady) getString(R.string.model_ready) else getString(R.string.model_missing))
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showStartupFailure(error)
            }
        }
    }

    private fun beginWork(): Boolean {
        if (indexing || IndexTaskCallbacks.running || work?.isActive == true) {
            toast(getString(R.string.index_busy))
            return false
        }
        return true
    }

    private fun endWork() {
        work = null
    }

    private fun setStatus(text: String, error: Boolean = false) {
        textStatus.text = text
        val color = com.google.android.material.color.MaterialColors.getColor(textStatus,
            if (error) com.google.android.material.R.attr.colorError else com.google.android.material.R.attr.colorOnSurface)
        textStatus.setTextColor(color)
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun fmtMb(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024.0))
}
