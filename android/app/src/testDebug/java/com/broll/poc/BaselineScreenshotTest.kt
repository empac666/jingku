package com.broll.poc

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.drawToBitmap
import androidx.recyclerview.widget.RecyclerView
import com.broll.poc.data.Segment
import com.broll.poc.ui.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import kotlinx.coroutines.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

/** Production XML/theme/renderer, deterministic state, no inference or real video fixture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BaselineScreenshotTest {
    @OptIn(ExperimentalRoborazziApi::class)
    @Test fun renderBaselineStates() {
        assumeTrue(System.getProperty("jingku.screenshots") == "true")
        val output = File(requireNotNull(System.getProperty("jingku.screenshotDir"))).apply { mkdirs() }
        val states = listOf("home-empty", "home-queued", "home-with-library", "home-pending", "model-preparing", "indexing-progress", "searching", "search-results", "no-dialogue", "search-failed", "playback", "import-sheet", "about-dialog", "crash-dialog")
        val selected = System.getProperty("jingku.screenshotStates").orEmpty().split(",").filter { it.isNotBlank() }
        for (language in listOf("zh", "en")) for (dark in listOf(false, true)) for (name in states) {
            if (selected.isNotEmpty() && name !in selected) continue
            org.robolectric.RuntimeEnvironment.setQualifiers("$language-w411dp-h891dp-${if (dark) "night" else "notnight"}-xxhdpi")
            val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
            val activity = controller.get()
            Settings.Global.putFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            val config = Configuration(activity.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            }
            val context = ContextThemeWrapper(activity, R.style.Theme_Jingku).apply { applyOverrideConfiguration(config) }
            val root = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
            activity.setContentView(root)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val screen = JingkuScreen(root, scope) { }
            val query = if (language == "zh") "日落" else "sunset"
            val line = if (language == "zh") "一起去看日落吧。" else "Let us watch the sunset together."
            val recents = listOf(query, if (language == "zh") "明天见" else "See you tomorrow")
            var state = ScreenState(opening = false, dbAvailable = true)
            when (name) {
                "home-queued" -> state = state.copy(queued = 2)
                "home-with-library" -> state = state.copy(count = 128, recents = recents)
                "home-pending" -> state = state.copy(count = 128, queued = 2, recents = recents)
                "model-preparing" -> state = state.copy(preparing = true, preparePercent = 42)
                "indexing-progress" -> state = state.copy(queued = 12, query = query, running = true, indexLine = context.getString(R.string.index_bar_title, 3, 12, "weekend.mp4"), indexPercent = 45)
                "searching" -> state = state.copy(query = query, searching = true, skeletonVisible = true)
                "search-results" -> state = state.copy(query = query, hits = List(2) { i -> Segment(i.toLong(), "content://fixture/$i", if (i == 0) "city-walk.mp4" else "weekend.mp4", "dialogue", 32.0, 36.0, line, if (i == 0) 0.85f else 0.71f) })
                "no-dialogue" -> state = state.copy(query = query, noDialogue = true, recents = recents)
                "search-failed" -> state = state.copy(query = query, failed = true)
                "playback" -> state = state.copy(playback = true, playLine = DialogueHighlighter.highlight(line, query, context.getColor(R.color.highlight_match), context.getColor(R.color.highlight_match_on)), playTime = "00:32")
                "import-sheet" -> state = state.copy(queued = 2)
            }
            screen.query.setText(state.query)
            // MainActivity sizes this well to 16:9 from its measured width before playback.
            if (name == "playback") root.findViewById<View>(R.id.playerWell).apply {
                layoutParams = layoutParams.apply { height = 1233 * 9 / 16 }
            }
            screen.render(state)
            shadowOf(Looper.getMainLooper()).idle()
            fun layout() {
                root.measure(View.MeasureSpec.makeMeasureSpec(1233, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2673, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, 1233, 2673)
            }
            layout(); shadowOf(Looper.getMainLooper()).idle(); layout()
            if (name == "search-results") {
                val list = root.findViewById<RecyclerView>(R.id.resultList)
                for (i in 0 until list.childCount) list.getChildAt(i).findViewById<ImageView>(R.id.imageThumb).setImageResource(R.drawable.fixture_still)
            }
            val file = File(output, "$name-$language-${if (dark) "dark" else "light"}.png").absolutePath
            when (name) {
                "import-sheet" -> {
                    val session = QueueSession().apply { pending.addAll(listOf(QueueVideo("content://fixture/0", "city-walk.mp4"), QueueVideo("content://fixture/1", "weekend.mp4"))) }
                    val sheet = ImportSheet(context, scope, session, {}, {}, {}, {})
                    sheet.render(false, null); sheet.show(); shadowOf(Looper.getMainLooper()).idle()
                    val list = sheet.root.findViewById<RecyclerView>(R.id.queueList)
                    for (i in 0 until list.childCount) list.getChildAt(i).findViewById<ImageView>(R.id.queueThumb).setImageResource(R.drawable.fixture_still)
                    captureScreenRoboImage(file); sheet.dispose()
                }
                "about-dialog" -> {
                    val body = context.assets.open("licenses/ABOUT-$language.txt").bufferedReader().use { it.readText() }
                    val dialog = ReadableDialogs.licenses(context, body)
                    shadowOf(Looper.getMainLooper()).idle(); captureScreenRoboImage(file); dialog.dismiss()
                }
                "crash-dialog" -> {
                    val dialog = MaterialAlertDialogBuilder(context).setTitle(R.string.crash_report_title)
                        .setView(ReadableDialogs.reportView(context, "Jingku 1.0.3 (4)\nAndroid 15 · arm64-v8a\n\nDemo exception: offline preparation interrupted\n    at com.broll.poc.model.ModelStore.prepare(ModelStore.kt:15)"))
                        .setCancelable(false).setPositiveButton(R.string.crash_copy, null).setNeutralButton(R.string.crash_share, null).setNegativeButton(R.string.crash_dismiss, null).show()
                    shadowOf(Looper.getMainLooper()).idle(); captureScreenRoboImage(file); dialog.dismiss()
                }
                else -> root.drawToBitmap().captureRoboImage(file)
            }
            screen.dispose(); scope.cancel(); controller.pause().stop().destroy()
        }
    }
}
