package app.reseam.manager.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.reseam.manager.data.platform.desktopDataFile
import java.util.Properties

fun createReseamDatabase(): ReseamDatabase {
    val dbFile = desktopDataFile("reseam.db").also { it.parentFile?.mkdirs() }
    val driver: SqlDriver = JdbcSqliteDriver(
        url = "jdbc:sqlite:${dbFile.absolutePath}",
        properties = Properties(),
    )
    val schema = ReseamDatabase.Schema
    val current = userVersion(driver)
    when {
        current == 0L -> {
            schema.create(driver)
            setUserVersion(driver, schema.version)
        }
        current < schema.version -> {
            schema.migrate(driver, current, schema.version)
            setUserVersion(driver, schema.version)
        }
        current > schema.version -> error(
            "Reseam database schema is at version $current, newer than supported ${schema.version}",
        )
    }
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
    return ReseamDatabase(driver)
}

private fun userVersion(driver: SqlDriver): Long =
    driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

private fun setUserVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version", 0)
}
