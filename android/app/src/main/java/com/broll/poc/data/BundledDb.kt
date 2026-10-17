package com.broll.poc.data

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.Closeable

/**
 * Thin wrapper around [BundledSQLiteDriver] (FTS5-enabled SQLite), used instead of the system SQLiteOpenHelper.
 */
class BundledDb private constructor(
    private val conn: SQLiteConnection,
) : Closeable {
    private var transactionDepth = 0
    private var transactionSuccessful = false

    fun execSQL(sql: String) {
        conn.prepare(sql).use { stmt ->
            stmt.step()
        }
    }

    fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        conn.prepare(sql).use { stmt ->
            bindAll(stmt, bindArgs)
            stmt.step()
        }
    }

    fun query(sql: String, bindArgs: Array<out String?>? = null): SqliteRows {
        val stmt = conn.prepare(sql)
        if (bindArgs != null) {
            bindAll(stmt, bindArgs)
        }
        return SqliteRows(stmt)
    }

    fun insert(table: String, nullColumnHack: String?, values: android.content.ContentValues): Long {
        val cols = values.keySet().toList()
        if (cols.isEmpty()) return -1L
        val placeholders = cols.joinToString(", ") { "?" }
        val sql = "INSERT INTO $table (${cols.joinToString(", ")}) VALUES ($placeholders)"
        conn.prepare(sql).use { stmt ->
            cols.forEachIndexed { i, col ->
                bindValue(stmt, i + 1, values.get(col))
            }
            stmt.step()
        }
        return query("SELECT last_insert_rowid()").use { c ->
            if (c.moveToNext()) c.getLong(0) else -1L
        }
    }

    fun delete(table: String, whereClause: String?, whereArgs: Array<String>?): Int {
        val sql = if (whereClause == null) {
            "DELETE FROM $table"
        } else {
            "DELETE FROM $table WHERE $whereClause"
        }
        conn.prepare(sql).use { stmt ->
            if (whereArgs != null) {
                whereArgs.forEachIndexed { i, arg -> stmt.bindText(i + 1, arg) }
            }
            stmt.step()
        }
        return 0
    }

    fun beginTransaction() {
        execSQL("BEGIN IMMEDIATE")
        transactionDepth++
        transactionSuccessful = false
    }

    fun setTransactionSuccessful() {
        transactionSuccessful = true
    }

    fun endTransaction() {
        if (transactionDepth > 0) {
            transactionDepth--
            if (transactionSuccessful) {
                execSQL("COMMIT")
            } else {
                execSQL("ROLLBACK")
            }
            transactionSuccessful = false
        }
    }

    fun userVersion(): Int = query("PRAGMA user_version").use { c ->
        if (c.moveToNext()) c.getInt(0) else 0
    }

    fun setUserVersion(version: Int) {
        execSQL("PRAGMA user_version = $version")
    }

    override fun close() {
        conn.close()
    }

    class SqliteRows(private val stmt: SQLiteStatement) : Closeable {
        fun moveToNext(): Boolean = stmt.step()

        fun getLong(columnIndex: Int): Long = stmt.getLong(columnIndex)

        fun getInt(columnIndex: Int): Int = stmt.getInt(columnIndex)

        fun getDouble(columnIndex: Int): Double = stmt.getDouble(columnIndex)

        fun getString(columnIndex: Int): String? = if (stmt.isNull(columnIndex)) null else stmt.getText(columnIndex)

        fun getBlob(columnIndex: Int): ByteArray = stmt.getBlob(columnIndex)

        override fun close() {
            stmt.close()
        }
    }

    companion object {
        private val driver = BundledSQLiteDriver()

        fun open(context: Context, name: String): BundledDb {
            val file = context.getDatabasePath(name)
            file.parentFile?.mkdirs()
            return open(file.absolutePath)
        }

        fun open(path: String): BundledDb {
            val flags = BundledSqliteOpen.READWRITE or BundledSqliteOpen.CREATE or BundledSqliteOpen.FULLMUTEX
            return BundledDb(driver.open(path, flags))
        }

        private fun bindAll(stmt: SQLiteStatement, args: Array<out Any?>) {
            args.forEachIndexed { i, arg ->
                bindValue(stmt, i + 1, arg)
            }
        }

        private fun bindValue(stmt: SQLiteStatement, index: Int, value: Any?) {
            when (value) {
                null -> stmt.bindNull(index)
                is String -> stmt.bindText(index, value)
                is Int -> stmt.bindLong(index, value.toLong())
                is Long -> stmt.bindLong(index, value)
                is Double -> stmt.bindDouble(index, value)
                is Float -> stmt.bindDouble(index, value.toDouble())
                is ByteArray -> stmt.bindBlob(index, value)
                else -> stmt.bindText(index, value.toString())
            }
        }
    }
}
