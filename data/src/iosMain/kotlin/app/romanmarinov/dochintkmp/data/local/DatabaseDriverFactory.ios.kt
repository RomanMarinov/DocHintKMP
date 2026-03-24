package app.romanmarinov.dochintkmp.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.romanmarinov.dochintkmp.persistence.DocHintDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = DocHintDatabase.Schema,
            name = "dochint.db"
        )
    }
}
