package app.reseam.manager.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.reseam.manager.data.platform.desktopDataFile
import java.util.Properties

fun createReseamDatabase(): ReseamDatabase {
    val dbFile = desktopDataFile("reseam.db").also { it.parentFile?.mkdirs() }
    val freshSchema = !dbFile.exists()
    val driver: SqlDriver = JdbcSqliteDriver(
        url = "jdbc:sqlite:${dbFile.absolutePath}",
        properties = Properties(),
    )
    if (freshSchema) {
        ReseamDatabase.Schema.create(driver)
    }
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
    return ReseamDatabase(driver)
}
