package com.broll.poc.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.broll.poc.R
import com.broll.poc.data.Segment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/** Main-thread state. The renderer owns the screen's visibility, copy and enabled state. */
data class ScreenState(
    val opening: Boolean = true, val dbAvailable: Boolean = false, val dbMessage: String = "",
    val count: Int = 0, val queued: Int = 0, val query: String = "",
    val preparing: Boolean = false, val preparePercent: Int? = null, val prepareError: String? = null,
    val playback: Boolean = false, val playLine: CharSequence = "", val playTime: String = "", val playNote: String = "", val playError: Boolean = false,
    val searching: Boolean = false, val skeletonVisible: Boolean = false,
    val resultQuery: String? = null, val failed: Boolean = false, val noDialogue: Boolean = false, val hits: List<Segment> = emptyList(),
    val running: Boolean = false, val indexLine: String? = null, val indexPercent: Int? = null,
    val indexError: Boolean = false, val cancelling: Boolean = false, val recents: List<String> = emptyList(),
)
enum class BodyAction { IMPORT, INDEX, RETRY, FIX_DB }

class JingkuScreen(val root: View, scope: CoroutineScope, onPlay: (Segment) -> Unit) {
    private val c = root.context
    val query: EditText = root.findViewById(R.id.editQuery)
    private fun v(id: Int): View = root.findViewById(id)
    private fun text(id: Int, value: CharSequence) { root.findViewById<TextView>(id).text = value }
    private fun show(id: Int, yes: Boolean) { v(id).isVisible = yes }
    private fun string(id: Int) = c.getString(id)
    private fun quantity(id: Int, n: Int) = c.resources.getQuantityString(id, n, n)
    var primaryAction = BodyAction.IMPORT
        private set
    var onRecent: (String) -> Unit = {}
    private val results = ResultAdapter(scope, onPlay)
    private var pulse: ObjectAnimator? = null
    private var recentValues: List<String>? = null
    private var lastOverlay = 0
    private var returnFocus: View? = null
    init {
        root.findViewById<RecyclerView>(R.id.resultList).apply { layoutManager = LinearLayoutManager(c); adapter = results }
        text(R.id.btnLanguage, string(if (c.resources.configuration.locales[0].language == "zh") R.string.lang_switch else R.string.lang_switch_zh))
    }
    fun render(s: ScreenState) {
        val overlay = if (s.preparing) R.id.overlayPreparing else if (s.playback) R.id.overlayPlayback else 0
        show(R.id.overlayPreparing, overlay == R.id.overlayPreparing)
        show(R.id.overlayPlayback, overlay == R.id.overlayPlayback)
        show(R.id.videoView, overlay == R.id.overlayPlayback)
        show(R.id.searchRow, overlay == 0)
        show(R.id.toolbar, overlay == 0)
        v(R.id.body).importantForAccessibility = if (overlay != 0) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        if (overlay != lastOverlay) {
            if (overlay != 0) {
                returnFocus = root.findFocus()
                val first = v(if (overlay == R.id.overlayPlayback) R.id.btnBack else R.id.prepTitle)
                first.isFocusableInTouchMode = true; first.requestFocus()
                if (Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f) {
                    val targets = if (overlay == R.id.overlayPreparing) listOf(R.id.overlayPreparing) else listOf(R.id.btnBack, R.id.playDialogue, R.id.playCaption, R.id.playNote)
                    targets.forEach { id -> v(id).apply { alpha = 0f; animate().alpha(1f).setDuration(200).setInterpolator(PathInterpolator(0f, 0f, 0f, 1f)).start() } }
                }
            } else returnFocus?.requestFocus()
            lastOverlay = overlay
        }
        val nonempty = s.query.isNotEmpty()
        show(R.id.btnClear, nonempty)
        show(R.id.sendTarget, nonempty && !s.failed)
        root.findViewById<MaterialButton>(R.id.btnSearch).apply {
            isEnabled = !s.running && !s.searching
            alpha = if (isEnabled) 1f else 0.38f
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(c, R.color.primary))
            iconTint = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(c, R.color.on_primary))
            contentDescription = string(if (s.running) R.string.search_unavailable else R.string.btn_search)
        }
        (v(R.id.searchRow) as MaterialCardView).apply {
            strokeWidth = ((if (query.hasFocus()) 2 else 1) * c.resources.displayMetrics.density).toInt()
            strokeColor = ContextCompat.getColor(c, if (query.hasFocus()) R.color.primary else R.color.outline)
        }
        val resultsVisible = !s.opening && s.dbAvailable && !s.searching && !s.failed && s.hits.isNotEmpty()
        val searchingVisible = !s.opening && s.dbAvailable && s.searching
        show(R.id.resultsBody, resultsVisible)
        show(R.id.searchingBody, searchingVisible && s.skeletonVisible)
        show(R.id.homeBody, !resultsVisible && !searchingVisible)
        if (resultsVisible) {
            text(R.id.resultHeader, quantity(R.plurals.result_shots, s.hits.size))
            results.submit(s.hits, s.resultQuery ?: s.query)
        }
        val queued = s.queued > 0 && s.count == 0 && !s.running
        val ready = s.count > 0
        val title: String
        val detail: String
        primaryAction = BodyAction.IMPORT
        var action = R.string.import_action
        when {
            s.opening -> { title = string(R.string.db_opening); detail = "" }
            !s.dbAvailable -> { title = string(R.string.status_db_unavailable); detail = s.dbMessage; action = R.string.db_fix; primaryAction = BodyAction.FIX_DB }
            s.failed -> { title = string(R.string.search_failed_title); detail = string(R.string.search_failed_body); action = R.string.retry; primaryAction = BodyAction.RETRY }
            s.running && !ready -> { title = string(R.string.index_running_title); detail = string(R.string.index_busy_line) }
            queued -> { title = quantity(R.plurals.selected_videos, s.queued); detail = string(R.string.queued_body); action = R.string.btn_start_index; primaryAction = BodyAction.INDEX }
            ready -> { title = quantity(R.plurals.searchable_lines, s.count); detail = "" }
            s.noDialogue -> { title = string(R.string.no_dialogue_title); detail = string(R.string.no_dialogue_body) }
            else -> { title = string(R.string.home_empty_title); detail = string(R.string.home_empty_body) }
        }
        text(R.id.bodyTitle, title); text(R.id.bodyDetail, detail)
        show(R.id.bodyDetail, detail.isNotBlank())
        root.findViewById<TextView>(R.id.bodyDetail).setTextColor(ContextCompat.getColor(c, if (!s.opening && !s.dbAvailable) R.color.error else R.color.on_surface_variant))
        show(R.id.emptyIcon, !s.opening && s.dbAvailable && !s.failed && !queued && !ready && !s.noDialogue)
        show(R.id.btnPrimary, !s.opening && !s.running && !s.noDialogue && (!ready || s.failed || !s.dbAvailable))
        text(R.id.btnPrimary, string(action))
        (v(R.id.btnPrimary) as MaterialButton).apply {
            val filled = primaryAction != BodyAction.FIX_DB && (!nonempty || s.failed)
            backgroundTintList = ContextCompat.getColorStateList(c, if (filled) R.color.primary else android.R.color.transparent)
            setTextColor(ContextCompat.getColor(c, if (filled) R.color.on_primary else R.color.primary))
        }
        show(R.id.btnSecondary, queued)
        show(R.id.pendingLine, ready && s.queued > 0)
        show(R.id.btnViewQueue, ready && s.queued > 0)
        text(R.id.pendingLine, quantity(R.plurals.pending_videos, s.queued))
        val recents = !s.searching && !s.failed && s.hits.isEmpty() && s.dbAvailable && s.recents.isNotEmpty() && (ready || s.noDialogue)
        show(R.id.recentTitle, recents); show(R.id.recents, recents)
        if (recentValues != s.recents) {
            recentValues = s.recents.toList()
            val group = v(R.id.recents) as ViewGroup
            group.removeAllViews()
            s.recents.forEach { q -> group.addView(Chip(c).apply {
                text = q; minHeight = dp(48); chipMinHeight = dp(48).toFloat(); chipStartPadding = dp(16).toFloat(); chipEndPadding = dp(16).toFloat(); setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge); maxWidth = dp(180); maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                isCheckable = false; isClickable = true
                shapeAppearanceModel = shapeAppearanceModel.withCornerSize(com.google.android.material.shape.RelativeCornerSize(0.5f))
                chipBackgroundColor = ContextCompat.getColorStateList(c, R.color.primary_container)
                setTextColor(ContextCompat.getColor(c, R.color.on_primary_container))
                chipStrokeWidth = 0f
                setOnClickListener { onRecent(q) }
            }) }
        }
        if (s.preparing) {
            text(R.id.prepDetail, s.prepareError ?: string(R.string.models_preparing_detail))
            text(R.id.prepPercent, if (s.preparePercent == null || s.preparePercent >= 100) string(R.string.models_verifying) else c.getString(R.string.models_unpacking, s.preparePercent))
            show(R.id.prepPercent, s.prepareError == null)
            progress(R.id.progressEmbed, s.preparePercent)
            show(R.id.progressEmbed, s.prepareError == null)
            show(R.id.btnRetryModels, s.prepareError != null); show(R.id.btnModelsLater, s.prepareError != null)
        }
        if (s.playback) {
            text(R.id.playDialogue, s.playLine)
            text(R.id.playCaption, if (s.playError) string(R.string.play_error) else c.getString(R.string.play_jumped, s.playTime))
            root.findViewById<TextView>(R.id.playCaption).setTextColor(ContextCompat.getColor(c, if (s.playError) R.color.error else R.color.on_surface_variant))
            text(R.id.playNote, s.playNote); show(R.id.playNote, s.playNote.isNotBlank() && !s.playError)
            show(R.id.btnPlayImport, s.playError)
        }
        val ime = androidx.core.view.ViewCompat.getRootWindowInsets(root)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
        show(R.id.indexBar, overlay == 0 && s.indexLine != null && !ime)
        text(R.id.indexTitle, s.indexLine.orEmpty())
        root.findViewById<TextView>(R.id.indexTitle).setTextColor(ContextCompat.getColor(c, if (s.indexError) R.color.error else R.color.on_surface))
        show(R.id.indexBusy, s.running)
        progress(R.id.progressIndex, if (s.cancelling) null else s.indexPercent)
        show(R.id.progressIndex, s.running)
        show(R.id.btnIndexRetry, s.indexError && s.queued > 0)
        show(R.id.btnIndexDismiss, s.indexError)
        val animate = searchingVisible && s.skeletonVisible && overlay == 0 && Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        if (animate && pulse == null) {
            pulse = ObjectAnimator.ofFloat(v(R.id.searchingBody), View.ALPHA, 0.45f, 1f).apply {
                duration = 1000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
                interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f); start()
            }
        } else if (!animate) { pulse?.cancel(); pulse = null; v(R.id.searchingBody).alpha = 1f }
    }
    private fun dp(n: Int) = (n * c.resources.displayMetrics.density).toInt()
    private fun progress(id: Int, percent: Int?) {
        root.findViewById<LinearProgressIndicator>(id).apply {
            if (isIndeterminate != (percent == null)) { hide(); isIndeterminate = percent == null; show() }
            if (percent != null) setProgressCompat(percent, false)
        }
    }
    fun dispose() { pulse?.cancel(); results.dispose() }
}

private class ResultAdapter(private val scope: CoroutineScope, private val onPlay: (Segment) -> Unit) : RecyclerView.Adapter<ResultAdapter.Holder>() {
    class Holder(val card: View) : RecyclerView.ViewHolder(card)
    private var hits: List<Segment> = emptyList()
    private var query = ""
    private val jobs = mutableMapOf<View, Job>()
    fun submit(values: List<Segment>, q: String) {
        if (values == hits && q == query) return
        hits = values; query = q; notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(SearchResultBinder.inflateCard(parent))
    override fun getItemCount() = hits.size
    override fun onBindViewHolder(holder: Holder, position: Int) = SearchResultBinder.bind(holder.card, hits[position], query, scope, onPlay, jobs)
    override fun onViewRecycled(holder: Holder) { jobs.remove(holder.card)?.cancel(); holder.card.findViewById<View>(R.id.imageThumb).tag = null }
    fun dispose() { jobs.values.forEach { it.cancel() }; jobs.clear() }
}
