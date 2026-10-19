package com.broll.poc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.broll.poc.data.IndexDatabase
import com.broll.poc.data.Segment
import com.broll.poc.embed.truncateNormalize
import com.broll.poc.metrics.MetricsLogger
import com.broll.poc.model.AsrModelStore
import com.broll.poc.model.ModelStore
import com.broll.poc.model.ModelPreparation
import com.broll.poc.ui.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null
    private var prepareJob: Job? = null
    private var hold: Job? = null
    private var generation = 0
    private var batchGeneration = 0
    private var destroyed = false
    private var startupFailed = false
    private lateinit var screen: JingkuScreen
    private lateinit var logger: MetricsLogger
    private lateinit var videoView: VideoView
    private var db: IndexDatabase? = null
    private var dbFailure = ""
    private var state = ScreenState()
    private var session = QueueSession()
    private var sheet: ImportSheet? = null
    private var modelsReady = false
    private var playingUri: Uri? = null
    private var preparedPlayer: android.media.MediaPlayer? = null
    private var pendingSeekMs = 0L
    private var lastSearchQuery: String? = null
        private var busyToastShown = false
    private var lastAnnouncedStage: IndexStage? = null
    private val pickGallery = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) addVideos(uris)
    }
    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) addVideos(uris)
    }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startIndexInternal() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(runCatching { LocaleStore.wrap(newBase) }.getOrDefault(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            (application as BrollApplication).startupFailure?.let { throw it }
            initializeUi(savedInstanceState)
        } catch (error: Throwable) { showStartupFailure(error) }
        showPreviousCrash()
    }
    private fun initializeUi(saved: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = resources.getBoolean(R.bool.light_system_bars)
            isAppearanceLightNavigationBars = resources.getBoolean(R.bool.light_system_bars)
        }
        setContentView(R.layout.activity_main)
        logger = MetricsLogger(this); EngineHolder.bindMetrics(logger)
        session = (lastCustomNonConfigurationInstance as? QueueSession) ?: QueueSession().also { q ->
            val uris = saved?.getStringArrayList("import_uris").orEmpty()
            val names = saved?.getStringArrayList("import_names").orEmpty()
            uris.forEachIndexed { i, uri -> q.pending.add(QueueVideo(uri, names.getOrNull(i) ?: uri)) }
        }
        screen = JingkuScreen(findViewById(R.id.root), scope, ::play)
        if (!IndexTaskCallbacks.running && session.successful) session.completeSuccess()
        state = state.copy(recents = loadRecents(), running = IndexTaskCallbacks.running, cancelling = IndexTaskCallbacks.running && saved?.getBoolean("cancelling") == true)
        screen.query.setText(saved?.getString("query").orEmpty())
        state = state.copy(query = screen.query.text.toString())
        screen.query.doAfterTextChanged { state = state.copy(query = it?.toString().orEmpty()); render() }
        screen.query.setOnFocusChangeListener { _, _ -> render() }
        screen.query.setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_SEARCH) { startSearch(); true } else false }
        screen.onRecent = { screen.query.setText(it); startSearch() }
        click(R.id.btnSearch) { startSearch() }
        click(R.id.btnClear) { cancelSearch(false); screen.query.text.clear(); home(); render() }
        click(R.id.btnLanguage) {
            LocaleStore.setLanguage(this, if (LocaleStore.getLanguage(this) == LocaleStore.LANG_ZH) LocaleStore.LANG_EN else LocaleStore.LANG_ZH)
            recreate()
        }
        click(R.id.btnAbout) { showLicenses() }
        findViewById<View>(R.id.toolbar).setOnLongClickListener { showLicenses(); true }
        click(R.id.btnLibrary) { openSheet() }
        click(R.id.btnPrimary) {
            when (screen.primaryAction) {
                BodyAction.IMPORT -> chooseImport()
                BodyAction.INDEX -> startIndex()
                BodyAction.RETRY -> startSearch(retry = true)
                BodyAction.FIX_DB -> showIndexRebuildDialog(dbFailure)
            }
        }
        click(R.id.btnSecondary) { chooseImport() }
        click(R.id.btnViewQueue) { openSheet() }
        click(R.id.indexBar) { openSheet() }
        click(R.id.btnIndexRetry) { startIndex() }
        click(R.id.btnIndexDismiss) { hold?.cancel(); session.clearBatch(); state = state.copy(indexLine = null, indexError = false); render() }
        click(R.id.btnRetryModels) { prepareModels() }
        click(R.id.btnModelsLater) { state = state.copy(preparing = false); render() }
        click(R.id.btnBack) { closePlayback() }
        click(R.id.btnPlayImport) { closePlayback(); chooseImport() }
        videoView = findViewById(R.id.videoView)
        val well = findViewById<FrameLayout>(R.id.playerWell)
        well.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val height = v.width * 9 / 16
            if (height > 0 && v.layoutParams.height != height) { v.layoutParams = v.layoutParams.apply { this.height = height } }
        }
        videoView.setMediaController(MediaController(this).apply { setAnchorView(well) })
        videoView.setOnErrorListener { _, _, _ ->
            preparedPlayer = null; playingUri = null
            state = state.copy(playError = true); render(); true
        }
        installInsets()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val insets = ViewCompat.getRootWindowInsets(screen.root)
                when {
                    insets?.isVisible(WindowInsetsCompat.Type.ime()) == true -> WindowCompat.getInsetsController(window, screen.root).hide(WindowInsetsCompat.Type.ime())
                    state.playback -> closePlayback()
                    sheet?.dialog?.isShowing == true -> sheet?.dialog?.dismiss()
                    state.searching -> cancelSearch(true)
                    state.hits.isNotEmpty() || state.failed || state.noDialogue -> { home(); render() }
                    state.preparing && state.prepareError == null -> Unit
                    state.preparing -> { state = state.copy(preparing = false); render() }
                    else -> finish()
                }
            }
        })
        wireIndexCallbacks()
        if (IndexTaskCallbacks.running) IndexTaskCallbacks.lastProgress?.let { receiveProgress(it) }
        else if (session.batch.isNotEmpty() && session.terminal == null) IndexTaskCallbacks.lastResult?.let { receiveFinished(it) }
        render(); prepareModels(); openDatabase()
    }
    private fun installInsets() {
        val pad = dp(20)
        ViewCompat.setOnApplyWindowInsetsListener(screen.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            findViewById<View>(R.id.toolbar).setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.indexBar).setPadding(pad + bars.left, pad, pad + bars.right, pad + bars.bottom)
            findViewById<View>(R.id.body).setPadding(bars.left, 0, bars.right, bars.bottom)
            findViewById<View>(R.id.overlayPreparing).setPadding(bars.left, bars.top, bars.right, bars.bottom)
            findViewById<View>(R.id.overlayPlayback).setPadding(bars.left, bars.top, bars.right, bars.bottom)
            render(); insets
        }
    }
    private fun click(id: Int, action: () -> Unit) {
        findViewById<View>(id).setOnClickListener { action() }
        if (id != R.id.indexBar) findViewById<View>(id).setOnTouchListener { v, event ->
            if (motionEnabled()) {
                val scale = if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) 0.96f else 1f
                if (event.actionMasked in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL))
                    v.animate().scaleX(scale).scaleY(scale).setDuration(100).setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f)).start()
            }; false
        }
    }
    private fun render() {
        if (destroyed || startupFailed || !::screen.isInitialized) return
        state = state.copy(queued = session.pending.size)
        screen.render(state)
        sheet?.render(state.running, if (state.running && session.active > 0) getString(R.string.sheet_indexing, session.active, IndexTaskCallbacks.lastProgress?.batchTotal ?: session.batch.size) else null)
    }
    private fun home() { state = state.copy(hits = emptyList(), failed = false, noDialogue = false, searching = false, skeletonVisible = false) }
    private fun openDatabase() {
        scope.launch {
            var pending: IndexDatabase? = null
            try {
                val (opened, failure) = withContext(Dispatchers.IO) { IndexDatabase.tryOpen(applicationContext).also { pending = it.first } }
                db = opened; pending = null
                dbFailure = failure?.message.orEmpty()
                state = state.copy(opening = false, dbAvailable = opened != null, dbMessage = dbFailure)
                refreshCount()
                if (failure != null) showIndexRebuildDialog(dbFailure)
                render()
            } catch (e: Throwable) { pending?.close(); if (e is CancellationException) throw e; showStartupFailure(e) }
        }
    }
    private suspend fun refreshCount() {
        if (db == null) return
        val app = applicationContext
        val count = withContext(Dispatchers.IO) {
            val (opened, failure) = IndexDatabase.tryOpen(app)
            val index = opened ?: error(failure?.message ?: "Index unavailable")
            try { index.countDialogue() } finally { index.close() }
        }
        if (state.count != count) state = state.copy(count = count, noDialogue = false)
    }
    private fun showIndexRebuildDialog(message: String) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.db_dialog_title).setMessage(message).setCancelable(false)
            .setPositiveButton(R.string.db_dialog_rebuild) { _, _ ->
                scope.launch {
                    val (opened, failure) = withContext(Dispatchers.IO) { db?.close(); IndexDatabase.wipeAndOpen(applicationContext) }
                    db = opened
                    if (failure != null) { dbFailure = failure.message; showIndexRebuildDialog(dbFailure) }
                    else { state = state.copy(count = 0, noDialogue = false, dbAvailable = true, dbMessage = ""); toast(getString(R.string.db_rebuild_hint)) }
                    render()
                }
            }.setNegativeButton(R.string.db_dialog_later) { _, _ -> state = state.copy(dbMessage = message); render() }.show()
    }
    private fun prepareModels() {
        if (prepareJob?.isActive == true) return
        prepareJob = scope.launch {
            try {
                val ready = withContext(Dispatchers.IO) {
                    val asr = AsrModelStore(applicationContext)
                    ModelStore(applicationContext).locate() != null && asr.isSenseVoiceReady() && asr.isVadReady()
                }
                if (!ready) {
                    val alreadyRunning = ModelPreparation.running()
                    val task = ModelPreparation.start(applicationContext)
                    state = state.copy(preparing = true, prepareError = null, preparePercent = if (alreadyRunning) null else 0); render()
                    while (!task.isCompleted) {
                        state = state.copy(preparePercent = if (alreadyRunning) null else ModelPreparation.percent); render(); delay(100)
                    }
                    task.await()
                    hapticConfirm()
                }
                modelsReady = true; state = state.copy(preparing = false, prepareError = null); render()
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                val message = getString(if (e is com.broll.poc.model.InsufficientModelSpace) R.string.models_storage_error else R.string.models_prepare_error)
                state = state.copy(preparing = true, prepareError = message); render()
                showReport(getString(R.string.startup_error_title), message + "\n\n" + CrashGuard.describe(e))
            }
        }
    }
    private fun chooseImport() {
        MaterialAlertDialogBuilder(this).setTitle(R.string.import_action).setItems(arrayOf(getString(R.string.btn_pick_gallery), getString(R.string.btn_pick_saf))) { _, which ->
            if (which == 0) pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) else pickFiles.launch(arrayOf("video/*"))
        }.show()
    }
    private fun addVideos(uris: List<Uri>) {
        scope.launch {
            val videos = withContext(Dispatchers.IO) { uris.map { uri ->
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
                val name = runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull() ?: uri.lastPathSegment ?: uri.toString()
                QueueVideo(uri.toString(), name)
            } }
            videos.forEach { v -> if (session.pending.none { it.uri == v.uri }) session.pending.add(v) }
            home(); render(); openSheet()
        }
    }
    private fun openSheet() {
        if (sheet?.dialog?.isShowing == true) return
        sheet?.dispose()
        sheet = ImportSheet(this, scope, session, ::chooseImport, ::startIndex, ::cancelIndex, ::render)
        render(); sheet?.show()
    }
    private fun startIndex() {
        if (state.preparing && state.prepareError == null) { toast(getString(R.string.models_preparing)); return }
        if (session.pending.isEmpty()) { toast(getString(R.string.index_need_video)); return }
        if (!modelsReady) { toast(getString(R.string.index_need_embed)); return }
        if (state.running || IndexTaskCallbacks.running || (work?.isCompleted == false || SearchLease.busy)) { toast(getString(R.string.index_busy)); return }
        if (db == null) { showIndexRebuildDialog(dbFailure); return }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        else startIndexInternal()
    }
    private fun startIndexInternal() {
        if (destroyed || state.running || IndexTaskCallbacks.running || session.pending.isEmpty() || (work?.isCompleted == false || SearchLease.busy)) return
        hold?.cancel(); batchGeneration++; session.start(); lastAnnouncedStage = null; busyToastShown = false
        state = state.copy(running = true, indexLine = getString(R.string.index_engine_init), indexPercent = null, indexError = false, cancelling = false)
        render(); openSheet()
        // Publish the pending start before Android dispatches ACTION_INDEX, so
        // rotation cannot replay a previous batch's terminal result in that gap.
        IndexTaskCallbacks.running = true
        IndexTaskCallbacks.lastProgress = null
        IndexTaskCallbacks.lastResult = null
        try { IndexForegroundService.startIndexBatch(this, session.batch.map { Uri.parse(it.uri) to it.name }) }
        catch (e: Exception) { logger.log(event = "index", stage = "error", detail = e.toString()); IndexTaskCallbacks.finish(IndexResult.Error(e.toString())) }
    }
    private fun cancelIndex() {
        if (!state.running) return
        state = state.copy(cancelling = true, indexLine = getString(R.string.index_cancelling)); render()
        IndexForegroundService.stopIndex(this)
    }
    private fun wireIndexCallbacks() {
        IndexTaskCallbacks.owner = this
        IndexTaskCallbacks.onStatus = { _ -> runOnUiThread { if (!destroyed) { session.stage = getString(R.string.index_engine_init); render() } } }
        IndexTaskCallbacks.onProgress = { progress -> runOnUiThread { if (!destroyed) receiveProgress(progress) } }
        IndexTaskCallbacks.onFinished = { result -> runOnUiThread { if (!destroyed) receiveFinished(result) } }
    }
    private fun receiveProgress(p: IndexProgress) {
        if (session.batch.isEmpty()) {
            session.replay = session.pending.firstOrNull { it.name == p.videoName } ?: QueueVideo("", p.videoName)
        }
        val stage = when (p.stage) {
            IndexStage.DECODE -> getString(R.string.index_stage_decode)
            IndexStage.TRANSCRIBE -> getString(R.string.index_stage_transcribe)
            IndexStage.EMBED -> getString(R.string.index_stage_embed, p.embedCurrent.toString(), p.embedTotal.toString())
        }
        session.observe(p.batchIndex); session.stage = stage; session.percent = p.embedPercent
        state = state.copy(running = true, indexPercent = p.embedPercent,
            indexLine = if (state.cancelling) getString(R.string.index_cancelling) else getString(R.string.index_bar_title, p.batchIndex, p.batchTotal, p.videoName))
        if (lastAnnouncedStage != p.stage) { announce(stage); lastAnnouncedStage = p.stage }
        render()
    }
    private fun receiveFinished(result: IndexResult) {
        val success = result is IndexResult.Success
        val error = result is IndexResult.Error
        val label = when (result) {
            is IndexResult.Success -> {
                session.batch.zip(result.summaries).forEach { (v, summary) -> session.counts[v.uri] = summary.dialogueSegments }
                val n = result.summaries.sumOf { it.dialogueSegments }
                if (n == 0) getString(R.string.index_zero_done) else resources.getQuantityString(R.plurals.index_done_lines, n, n)
            }
            is IndexResult.Cancelled -> getString(R.string.index_cancelled)
            is IndexResult.Error -> { logger.log(event = "index", stage = "error", detail = result.message); getString(R.string.index_failed) }
        }
        session.finish(success, if (error) getString(R.string.index_failed) else getString(R.string.row_cancelled))
        state = state.copy(running = false, cancelling = false, indexLine = label, indexError = error)
        announce(label); hapticConfirm(); render()
        scope.launch {
            try { refreshCount(); render() }
            catch (e: Throwable) {
                if (e is CancellationException) throw e
                logger.log(event = "index", stage = "count_error", detail = e.toString())
                dbFailure = getString(R.string.status_db_unavailable)
                state = state.copy(dbAvailable = false, dbMessage = dbFailure); render()
            }
        }
        hold?.cancel()
        val id = ++batchGeneration
        if (!error) hold = scope.launch {
            delay(2000)
            if (id != batchGeneration) return@launch
            if (motionEnabled()) { findViewById<View>(R.id.indexBar).animate().alpha(0f).setDuration(200).start(); delay(200) }
            if (id != batchGeneration) return@launch
            if (success) session.completeSuccess() else session.clearBatch()
            state = state.copy(indexLine = null); findViewById<View>(R.id.indexBar).alpha = 1f; render()
        }
    }
    private fun startSearch(retry: Boolean = false) {
        if (state.running || IndexTaskCallbacks.running) { if (!busyToastShown) { toast(getString(R.string.index_busy)); busyToastShown = true }; return }
        if (state.searching || (work?.isCompleted == false || SearchLease.busy)) return
        val raw = if (retry) lastSearchQuery.orEmpty() else screen.query.text.toString().trim()
        if (retry) screen.query.setText(raw)
        if (raw.isEmpty()) return
        if (!modelsReady) { toast(getString(R.string.index_need_embed)); return }
        if (db == null) { showIndexRebuildDialog(dbFailure); return }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(screen.query.windowToken, 0)
        screen.query.clearFocus(); haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        lastSearchQuery = raw
        if (state.count == 0) { home(); state = state.copy(noDialogue = true); render(); return }
        val id = ++generation
        state = state.copy(searching = true, skeletonVisible = false, hits = emptyList(), failed = false, noDialogue = false); render()
        scope.launch { delay(150); if (id == generation && state.searching) { state = state.copy(skeletonVisible = true); render() } }
        work = scope.launch {
            try {
                val t0 = System.nanoTime()
                val app = applicationContext
                val hits = withContext(Dispatchers.Default) {
                    check(SearchLease.acquire()) { "Search engine in use" }
                    try {
                    val engine = EngineHolder.open()
                    val (opened, failure) = IndexDatabase.tryOpen(app)
                    val index = opened ?: error(failure?.message ?: "Index unavailable")
                    try {
                        index.requireSearchModeCompatible(engine.mode)
                        val vector = engine.embedText(AppConfig.QUERY_PREFIX + raw)
                        index.searchSemantic768(truncateNormalize(vector, AppConfig.FULL_DIM), AppConfig.SEARCH_TOP_K)
                    } finally { index.close() }
                    } finally { SearchLease.release() }
                }
                if (id != generation) return@launch
                logger.log(event = "search", stage = "semantic768", elapsedMs = (System.nanoTime() - t0) / 1_000_000, detail = "hits=${hits.size}")
                if (hits.isNotEmpty()) saveRecent(raw)
                state = state.copy(searching = false, hits = hits, resultQuery = raw, noDialogue = hits.isEmpty(), recents = loadRecents())
                render(); announce(if (hits.isEmpty()) getString(R.string.no_dialogue_title) else resources.getQuantityString(R.plurals.result_shots, hits.size, hits.size))
            } catch (e: CancellationException) { /* Cancel UI is owned by the generation that invalidated this job. */ }
            catch (e: Throwable) {
                if (id == generation) { logger.log(event = "search", stage = "error", detail = e.toString()); state = state.copy(searching = false, failed = true); render(); announce(getString(R.string.search_failed_title)) }
            }
        }
    }
    private fun cancelSearch(announce: Boolean) {
        generation++; work?.cancel(); home(); render()
        if (announce) announce(getString(R.string.search_cancelled))
    }
    private fun loadRecents(): List<String> = runCatching {
        val a = JSONArray(getSharedPreferences("recent_searches", MODE_PRIVATE).getString("queries", "[]"))
        (0 until a.length()).map { a.getString(it) }.take(8)
    }.getOrDefault(emptyList())
    private fun saveRecent(raw: String) {
        val q = raw.trim().take(80)
        val values = (listOf(q) + loadRecents().filter { it != q }).take(8)
        getSharedPreferences("recent_searches", MODE_PRIVATE).edit().putString("queries", JSONArray(values).toString()).apply()
    }
    private fun play(seg: Segment) {
        haptic(HapticFeedbackConstants.CONTEXT_CLICK)
        val uri = Uri.parse(seg.videoUri)
        pendingSeekMs = (seg.seekSecForQuery(lastSearchQuery) * 1000.0).toLong().coerceAtLeast(0)
        state = state.copy(playback = true, playError = false,
            playLine = DialogueHighlighter.highlight(seg.label, lastSearchQuery.orEmpty(), ContextCompat.getColor(this, R.color.highlight_match), ContextCompat.getColor(this, R.color.highlight_match_on)),
            playTime = SearchResultBinder.timestamp(pendingSeekMs / 1000.0),
            playNote = if (seg.seekAlignmentNote(lastSearchQuery).isBlank()) "" else getString(R.string.segment_start_unaligned))
        render()
        if (playingUri != uri) {
            playingUri = uri; preparedPlayer = null
            videoView.setOnPreparedListener { player ->
                preparedPlayer = player
                val well = findViewById<View>(R.id.playerWell)
                fun fit() {
                    val w = well.width; val h = well.height
                    if (w > 0 && h > 0 && player.videoWidth > 0 && player.videoHeight > 0) {
                        val scale = minOf(w.toFloat() / player.videoWidth, h.toFloat() / player.videoHeight)
                        videoView.layoutParams = FrameLayout.LayoutParams((player.videoWidth * scale).toInt(), (player.videoHeight * scale).toInt(), android.view.Gravity.CENTER)
                    }
                }
                player.setOnVideoSizeChangedListener { _, _, _ -> fit() }; well.post { fit() }
                player.setOnSeekCompleteListener { it.start() }; player.seekTo(pendingSeekMs, android.media.MediaPlayer.SEEK_CLOSEST)
            }
            videoView.setVideoURI(uri)
        } else preparedPlayer?.let { it.setOnSeekCompleteListener { p -> p.start() }; it.seekTo(pendingSeekMs, android.media.MediaPlayer.SEEK_CLOSEST) }
    }
    private fun closePlayback() {
        videoView.stopPlayback(); videoView.visibility = View.GONE; playingUri = null; preparedPlayer = null
        state = state.copy(playback = false); render()
        if (motionEnabled()) findViewById<View>(R.id.chrome).apply { alpha = 0f; animate().alpha(1f).setDuration(200).setInterpolator(PathInterpolator(0f, 0f, 0f, 1f)).start() }
    }
    private fun showLicenses() {
        scope.launch {
            val body = withContext(Dispatchers.IO) { assets.open("licenses/" + if (resources.configuration.locales[0].language == "en") "ABOUT-en.txt" else "ABOUT-zh.txt").bufferedReader().use { it.readText() } }
            com.broll.poc.ui.ReadableDialogs.licenses(this@MainActivity, body)
        }
    }
    private fun showStartupFailure(error: Throwable) {
        startupFailed = true; IndexTaskCallbacks.detach(this); scope.cancel()
        val message = getString(R.string.startup_failed) + "\n\n" + CrashGuard.describe(error)
        val text = TextView(this).apply { this.text = message; setPadding(dp(24), dp(24), dp(24), dp(24)) }
        setContentView(ScrollView(this).apply { addView(text) })
        showReport(getString(R.string.startup_error_title), message)
    }
    private fun showPreviousCrash() {
        val file = java.io.File(filesDir, "crash/last_crash.txt")
        val trace = runCatching { if (file.isFile) file.readText() else null }.getOrNull() ?: return
        showReport(getString(R.string.crash_report_title), trace) { file.delete() }
    }
    private fun showReport(title: String, trace: String, dismiss: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(this).setTitle(title).setView(com.broll.poc.ui.ReadableDialogs.reportView(this, trace)).setCancelable(false)
            .setPositiveButton(R.string.crash_copy) { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(title, trace))
            }.setNeutralButton(R.string.crash_share) { _, _ ->
                runCatching { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, trace), title)) }
                    .onFailure { toast(getString(R.string.crash_share_unavailable)) }
            }.setNegativeButton(R.string.crash_dismiss) { _, _ -> dismiss?.invoke() }.show()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        if (::screen.isInitialized) {
            outState.putStringArrayList("import_uris", ArrayList(session.pending.map { it.uri }))
            outState.putStringArrayList("import_names", ArrayList(session.pending.map { it.name }))
            outState.putString("query", screen.query.text.toString())
            outState.putBoolean("cancelling", state.cancelling)
        }
        super.onSaveInstanceState(outState)
    }
    override fun onRetainCustomNonConfigurationInstance(): Any = session
    override fun onDestroy() {
        destroyed = true; generation++; batchGeneration++; IndexTaskCallbacks.detach(this)
        if (::videoView.isInitialized) videoView.stopPlayback()
        if (::screen.isInitialized) screen.dispose()
        sheet?.dispose(); scope.cancel()
        // A cancelled native search can still be unwinding; it owns its own database handle.
        runCatching { db?.close() }; db = null
        super.onDestroy()
    }
    private fun announce(text: String) { if (::screen.isInitialized) screen.root.announceForAccessibility(text) }
    private fun haptic(value: Int) { if (Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0) screen.root.performHapticFeedback(value) }
    private fun hapticConfirm() { haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK) }
    private fun motionEnabled() = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
}
