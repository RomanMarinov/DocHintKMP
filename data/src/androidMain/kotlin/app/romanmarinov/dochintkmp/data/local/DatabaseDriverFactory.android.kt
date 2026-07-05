package app.romanmarinov.dochintkmp.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.romanmarinov.dochintkmp.persistence.DocHintDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = DocHintDatabase.Schema,
            context = context,
            name = "dochint.db"
        )
    }
}
