package app.romanmarinov.dochintkmp.shared.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Call from iOS App entry point before any screen uses DocumentListController.
 */
fun initKoinForIos(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(iosDocumentsModule)
    }
}
