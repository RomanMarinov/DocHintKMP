package app.romanmarinov.dochintkmp.shared.di

import org.koin.core.module.Module

/**
 * Koin module for iOS — Results + Offline Scanner.
 * On Android this is empty (Android uses composeApp's dataModule).
 */
expect val iosDocumentsModule: Module
