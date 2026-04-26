package app.reseam.manager.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createReseamDatabase(context: Context): ReseamDatabase {
    val driver: SqlDriver = AndroidSqliteDriver(
        schema = ReseamDatabase.Schema,
        context = context,
        name = "reseam.db",
    )
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
    return ReseamDatabase(driver)
}
