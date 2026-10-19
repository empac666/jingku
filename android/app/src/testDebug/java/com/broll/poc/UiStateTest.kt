package com.broll.poc

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.broll.poc.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UiStateTest {
    @Test fun readyHomeBeatsQueueAndIndexDisablesOnlySubmit() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Jingku)
        val root = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
        val screen = JingkuScreen(root, scope) {}
        screen.render(ScreenState(opening = false, dbAvailable = true, count = 128, queued = 2, running = true, query = "sunset"))
        assertEquals(context.resources.getQuantityString(R.plurals.searchable_lines, 128, 128), root.findViewById<TextView>(R.id.bodyTitle).text.toString())
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.pendingLine).visibility)
        assertFalse(root.findViewById<View>(R.id.btnSearch).isEnabled)
        assertTrue(screen.query.isEnabled)
        screen.dispose(); scope.cancel()
    }
    @Test fun runningEmptyLibraryNeverOffersAnIdleIndexAction() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Jingku)
        val root = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
        val screen = JingkuScreen(root, scope) {}
        screen.render(ScreenState(opening = false, dbAvailable = true, queued = 12, running = true))
        assertEquals(context.getString(R.string.index_running_title), root.findViewById<TextView>(R.id.bodyTitle).text)
        assertEquals(View.GONE, root.findViewById<View>(R.id.btnPrimary).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.btnSecondary).visibility)
        screen.render(ScreenState(opening = false, dbAvailable = true, noDialogue = true, recents = listOf("sunset")))
        assertEquals(View.GONE, root.findViewById<View>(R.id.btnPrimary).visibility)
        val group = root.findViewById<android.view.ViewGroup>(R.id.recents)
        val chip = group.getChildAt(0) as com.google.android.material.chip.Chip
        assertEquals(48 * context.resources.displayMetrics.density, chip.chipMinHeight, 0.1f)
        screen.dispose(); scope.cancel()
    }
    @Test fun preparationWinsOverPlaybackAndHidesBodyFromAccessibility() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Jingku)
        val root = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
        val screen = JingkuScreen(root, scope) {}
        screen.render(ScreenState(preparing = true, preparePercent = 100, playback = true))
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.overlayPreparing).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.overlayPlayback).visibility)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, root.findViewById<View>(R.id.body).importantForAccessibility)
        assertEquals(context.getString(R.string.models_verifying), root.findViewById<TextView>(R.id.prepPercent).text.toString())
        screen.dispose(); scope.cancel()
    }
    @Test fun literalHighlightIncludesBothColorsAndNoFalseMark() {
        val marked = DialogueHighlighter.highlight("Watch the sunset", "sunset", 1, 2) as android.text.Spanned
        assertEquals(1, marked.getSpans(0, marked.length, android.text.style.BackgroundColorSpan::class.java).size)
        assertEquals(2, marked.getSpans(0, marked.length, android.text.style.ForegroundColorSpan::class.java).single().foregroundColor)
        assertFalse(DialogueHighlighter.highlight("Watch the sunset", "rain", 1, 2) is android.text.Spanned)
    }
}
