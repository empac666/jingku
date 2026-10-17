package com.broll.poc.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BundledDbTransactionTest {
    @Test
    fun failedTransaction_rollsBack() {
        val path = File.createTempFile("bundled_tx_", ".db").absolutePath
        val db = BundledDb.open(path)
        try {
            db.execSQL("CREATE TABLE t(id INTEGER PRIMARY KEY, v TEXT)")
            db.beginTransaction()
            db.execSQL("INSERT INTO t(v) VALUES (?)", arrayOf("a"))
            db.setTransactionSuccessful()
            db.endTransaction()
            db.query("SELECT COUNT(*) FROM t", null).use { c ->
                c.moveToNext()
                assertEquals(1, c.getInt(0))
            }

            db.beginTransaction()
            db.execSQL("INSERT INTO t(v) VALUES (?)", arrayOf("b"))
            // no setTransactionSuccessful()
            db.endTransaction()
            db.query("SELECT COUNT(*) FROM t", null).use { c ->
                c.moveToNext()
                assertEquals(1, c.getInt(0))
            }
        } finally {
            db.close()
            File(path).delete()
        }
    }

    @Test
    fun successfulTransaction_commits() {
        val path = File.createTempFile("bundled_tx_ok_", ".db").absolutePath
        val db = BundledDb.open(path)
        try {
            db.execSQL("CREATE TABLE t(id INTEGER PRIMARY KEY, v TEXT)")
            db.beginTransaction()
            db.execSQL("INSERT INTO t(v) VALUES (?)", arrayOf("x"))
            db.setTransactionSuccessful()
            db.endTransaction()
            db.query("SELECT COUNT(*) FROM t", null).use { c ->
                c.moveToNext()
                assertEquals(1, c.getInt(0))
            }
        } finally {
            db.close()
            File(path).delete()
        }
    }
}
