package com.broll.poc

import com.broll.poc.ui.QueueSession
import com.broll.poc.ui.QueueVideo
import org.junit.Assert.*
import org.junit.Test

class QueueSessionTest {
    private val videos = (1..4).map { QueueVideo("content://video/$it", "$it.mp4") }
    @Test fun successRemovesSnapshotIncludingZeroDialogueAndKeepsNewPicks() {
        val q = QueueSession(); q.pending.addAll(videos.take(3)); q.start()
        q.pending.add(videos[3]); q.counts[videos[0].uri] = 0
        q.finish(true, "done")
        assertEquals(4, q.pending.size) // stays through the success hold
        q.completeSuccess(); assertEquals(listOf(videos[3]), q.pending)
    }
    @Test fun cancelRemovesOnlyUrisProvenByLaterProgress() {
        val q = QueueSession(); q.pending.addAll(videos); q.start(); q.observe(3)
        q.finish(false, "cancelled")
        assertEquals(videos.drop(2), q.pending)
        assertFalse(q.counts.containsKey(videos[2].uri))
    }
    @Test fun errorBeforeProgressKeepsWholeQueue() {
        val q = QueueSession(); q.pending.addAll(videos); q.start(); q.finish(false, "error")
        assertEquals(videos, q.pending)
    }
    @Test fun freshSessionDoesNotInventCompletedHistory() {
        val q = QueueSession(); q.pending.addAll(videos); q.observe(3)
        assertTrue(q.batch.isEmpty()); assertTrue(q.counts.isEmpty()); assertEquals(videos, q.rows())
    }
}
