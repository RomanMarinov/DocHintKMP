package app.romanmarinov.dochintkmp.di

import app.romanmarinov.dochintkmp.data.local.DatabaseDriverFactory
import app.romanmarinov.dochintkmp.data.local.ApiKeyStorage
import app.romanmarinov.dochintkmp.data.local.ProcessedHashStorage
import app.romanmarinov.dochintkmp.data.local.createProcessedHashStorage
import app.romanmarinov.dochintkmp.data.local.SecureStorage
import app.romanmarinov.dochintkmp.data.ocr.DocxTextExtractor
import app.romanmarinov.dochintkmp.data.ocr.OcrEngine
import app.romanmarinov.dochintkmp.data.ocr.PdfPageRenderer
import app.romanmarinov.dochintkmp.data.ocr.PdfFirstPageRenderer
import app.romanmarinov.dochintkmp.data.ocr.TesseractOcrEngine
import app.romanmarinov.dochintkmp.data.ocr.paddle.PaddleOcrEngine
import app.romanmarinov.dochintkmp.data.parser.RuleParser
import app.romanmarinov.dochintkmp.data.remote.OpenRouterClient
import app.romanmarinov.dochintkmp.data.remote.createOpenRouterHttpClient
import app.romanmarinov.dochintkmp.data.repository.MedicalRepositoryImpl
import app.romanmarinov.dochintkmp.data.repository.ResultsRepositoryImpl
import app.romanmarinov.dochintkmp.domain.repository.MedicalRepository
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ExtractTextFromImageUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ParseWithLlmUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import app.romanmarinov.dochintkmp.data.usecase.ExtractTextOfflineUseCase
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.OcrAiScannerViewModel
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.AiScannerPort
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.AiScannerPortImpl
import app.romanmarinov.dochintkmp.presentation.documents_screen.ResultsViewModel
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.OcrOfflineScannerViewModel
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.OcrOfflineScannerPort
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.OcrOfflineScannerPortImpl
import app.romanmarinov.dochintkmp.presentation.settings_screen.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {

    single { DatabaseDriverFactory(androidContext()) }
    single<ProcessedHashStorage> {
        createProcessedHashStorage {
            androidContext().filesDir.resolve("processed_hashes.preferences_pb").absolutePath
        }
    }
    single { SecureStorage(androidContext()) }
    single<ApiKeyStorage> { get<SecureStorage>() }

    single<OcrEngine> { TesseractOcrEngine(androidContext()) }

    single { PaddleOcrEngine(androidContext()) }

    single { PdfPageRenderer(androidContext()) }
    single<PdfFirstPageRenderer> { get<PdfPageRenderer>() }

    single { DocxTextExtractor(androidContext()) }

    single { RuleParser() }

    single { OpenRouterClient(createOpenRouterHttpClient()) }

    single<MedicalRepository> {
        MedicalRepositoryImpl(get(), get())
    }

    single<ResultsRepository> { ResultsRepositoryImpl(get(), get()) }

    factory {
        ExtractTextOfflineUseCase(
            get<PaddleOcrEngine>(),
            get<PdfPageRenderer>(),
            get<DocxTextExtractor>(),
            androidContext().contentResolver
        )
    }
}

val domainModule = module {
    factory { ExtractTextFromImageUseCase(get()) }
    factory { ParseWithLlmUseCase(get()) }
    factory { ObserveResultsUseCase(get()) }
    factory { CheckDuplicateUseCase(get()) }
    factory { AddResultUseCase(get()) }
    factory { RemoveResultUseCase(get()) }
    factory { ClearResultsUseCase(get()) }
}

val viewModelModule = module {
    factory<AiScannerPort> {
        AiScannerPortImpl(
            get<ExtractTextFromImageUseCase>(),
            get<ExtractTextOfflineUseCase>(),
            get<ParseWithLlmUseCase>(),
            get<CheckDuplicateUseCase>(),
            get<AddResultUseCase>(),
        )
    }
    viewModel {
        OcrAiScannerViewModel(
            get<AiScannerPort>(),
            get<ApiKeyStorage>(),
            get<PdfFirstPageRenderer>()
        )
    }

    factory<OcrOfflineScannerPort> {
        OcrOfflineScannerPortImpl(
            get<ExtractTextOfflineUseCase>(),
            get<CheckDuplicateUseCase>(),
            get<RuleParser>(),
            get<AddResultUseCase>(),
        )
    }
    viewModel { ResultsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel {
        OcrOfflineScannerViewModel(
            get<OcrOfflineScannerPort>(),
            get<PdfFirstPageRenderer>()
        )
    }
}
