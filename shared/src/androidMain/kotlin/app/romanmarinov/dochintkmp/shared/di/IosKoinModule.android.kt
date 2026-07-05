package app.romanmarinov.dochintkmp.shared.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Empty module — Android uses composeApp's dataModule instead.
 * This is never loaded on Android; initKoinForIos is only called from iOS.
 */
actual val iosDocumentsModule: Module = module { }
