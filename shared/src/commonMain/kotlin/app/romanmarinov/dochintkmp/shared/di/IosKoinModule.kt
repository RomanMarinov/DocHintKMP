package app.romanmarinov.dochintkmp.shared.di

import app.romanmarinov.dochintkmp.data.parser.RuleParser
import app.romanmarinov.dochintkmp.data.repository.ResultsRepositoryImpl
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import org.koin.dsl.module

/**
 * Koin module for iOS — Results + Offline Scanner (shared code).
 * No Android-specific deps (SecureStorage, OCR, etc.).
 */
val iosDocumentsModule = module {
    single<ResultsRepository> { ResultsRepositoryImpl() }
    factory { ObserveResultsUseCase(get()) }
    factory { RemoveResultUseCase(get()) }
    factory { ClearResultsUseCase(get()) }
    single { RuleParser() }
    factory { CheckDuplicateUseCase(get()) }
    factory { AddResultUseCase(get()) }
}
