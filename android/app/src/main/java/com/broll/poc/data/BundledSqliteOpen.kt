package com.broll.poc.data

/**
 * SQLite open flags for [androidx.sqlite.driver.bundled.BundledSQLiteDriver.open].
 * Loaded from [androidx.sqlite.driver.bundled.BundledSQLite] (sqlite-bundled 2.7.1) so values stay aligned with the driver.
 */
object BundledSqliteOpen {
    val READWRITE: Int
    val CREATE: Int
    val FULLMUTEX: Int

    init {
        val clazz = Class.forName("androidx.sqlite.driver.bundled.BundledSQLite")
        READWRITE = clazz.getField("SQLITE_OPEN_READWRITE").getInt(null)
        CREATE = clazz.getField("SQLITE_OPEN_CREATE").getInt(null)
        FULLMUTEX = clazz.getField("SQLITE_OPEN_FULLMUTEX").getInt(null)
    }
}
