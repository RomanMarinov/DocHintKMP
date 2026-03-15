package app.romanmarinov.androidapp

import android.app.Application
import app.romanmarinov.dochintkmp.di.dataModule
import app.romanmarinov.dochintkmp.di.domainModule
import app.romanmarinov.dochintkmp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DocHintApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DocHintApp)
            modules(dataModule, domainModule, viewModelModule)
        }
    }
}
