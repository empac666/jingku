package com.broll.poc.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.broll.poc.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImportSheet(context: Context, scope: CoroutineScope, private val session: QueueSession,
    pick: () -> Unit, start: () -> Unit, cancel: () -> Unit, private val changed: () -> Unit,
) {
    val dialog = BottomSheetDialog(context)
    val root: View = LayoutInflater.from(context).inflate(R.layout.sheet_import, null)
    private var isRunning = false
    private var currentTitle: String? = null
    private val adapter = QueueAdapter(scope, session) { video -> session.pending.remove(video); changed(); render(isRunning, currentTitle) }
    init {
        dialog.setContentView(root)
        root.findViewById<RecyclerView>(R.id.queueList).apply { layoutManager = LinearLayoutManager(context); adapter = this@ImportSheet.adapter }
        root.findViewById<View>(R.id.btnSheetImport).setOnClickListener { pick() }
        root.findViewById<View>(R.id.btnStartIndex).setOnClickListener { start() }
        root.findViewById<View>(R.id.btnCancelIndex).setOnClickListener { cancel() }
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime()).bottom
            root.setPadding(pad, pad, pad, pad + bottom); insets
        }
        dialog.setOnDismissListener { adapter.dispose() }
    }
    fun render(running: Boolean, title: String?) {
        isRunning = running; currentTitle = title
        root.findViewById<TextView>(R.id.sheetTitle).text = title ?: root.context.getString(R.string.import_title)
        root.findViewById<View>(R.id.sheetEmpty).isVisible = session.rows().isEmpty()
        root.findViewById<View>(R.id.queueList).isVisible = session.rows().isNotEmpty()
        root.findViewById<View>(R.id.btnStartIndex).isVisible = !running && session.pending.isNotEmpty()
        root.findViewById<View>(R.id.btnCancelIndex).isVisible = running
        adapter.running = running; adapter.notifyDataSetChanged()
    }
    fun show() { dialog.show(); dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED }
    fun dispose() { adapter.dispose(); dialog.dismiss() }
}

private class QueueAdapter(val scope: CoroutineScope, val session: QueueSession, val remove: (QueueVideo) -> Unit) : RecyclerView.Adapter<QueueAdapter.Holder>() {
    class Holder(val root: View) : RecyclerView.ViewHolder(root) { var job: Job? = null }
    var running = false
    private val holders = mutableSetOf<Holder>()
    override fun getItemCount() = session.rows().size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_queue, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holders.add(holder)
        val video = session.rows()[position]
        val index = session.batch.indexOf(video) + 1
        val active = running && (video == session.replay || (index > 0 && index == session.active))
        val c = holder.root.context
        holder.root.findViewById<TextView>(R.id.queueName).text = video.name
        val count = session.counts[video.uri]
        val stage = when {
            count != null -> if (count == 0) c.getString(R.string.row_zero) else c.resources.getQuantityString(R.plurals.row_done_lines, count, count)
            active -> session.stage.ifBlank { c.getString(R.string.index_engine_init) }
            index > 0 && index < session.lastObserved -> video.name
            index > 0 && index == session.active && session.terminal != null -> session.terminal
            running && index > 0 && session.active == 0 -> c.getString(R.string.index_engine_init)
            else -> video.name
        }
        holder.root.findViewById<TextView>(R.id.queueStage).apply { text = stage; isVisible = stage != video.name }
        holder.root.findViewById<View>(R.id.btnRemove).apply {
            isVisible = !active && (!running || index == 0 || index > session.active)
            setOnClickListener { remove(video) }
        }
        holder.root.findViewById<LinearProgressIndicator>(R.id.queueProgress).apply {
            isIndeterminate = session.percent == null
            if (session.percent != null) progress = session.percent!!
            isVisible = active
        }
        holder.job?.cancel()
        val thumb = holder.root.findViewById<ImageView>(R.id.queueThumb)
        thumb.tag = video.uri; thumb.setImageDrawable(null)
        holder.job = scope.launch {
            val bitmap = VideoFrameThumb.load(c, video.uri, 0.0, 720)
            if (bitmap != null && thumb.tag == video.uri) thumb.setImageBitmap(bitmap)
        }
    }
    override fun onViewRecycled(holder: Holder) { holder.job?.cancel(); holder.root.findViewById<View>(R.id.queueThumb).tag = null; holders.remove(holder) }
    fun dispose() { holders.forEach { it.job?.cancel() }; holders.clear() }
}
