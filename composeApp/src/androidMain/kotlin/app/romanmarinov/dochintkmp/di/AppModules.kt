package app.romanmarinov.dochintkmp.di

import app.romanmarinov.dochintkmp.data.local.SecureStorage
import app.romanmarinov.dochintkmp.data.ocr.DocxTextExtractor
import app.romanmarinov.dochintkmp.data.ocr.OcrEngine
import app.romanmarinov.dochintkmp.data.ocr.PdfPageRenderer
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
import app.romanmarinov.dochintkmp.presentation.ai_scanner.OcrAiScannerViewModel
import app.romanmarinov.dochintkmp.presentation.documents.ResultsViewModel
import app.romanmarinov.dochintkmp.presentation.offline_scanner.OcrOfflineScannerViewModel
import app.romanmarinov.dochintkmp.presentation.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {

    single { SecureStorage(androidContext()) }

    single<OcrEngine> { TesseractOcrEngine(androidContext()) }

    single { PaddleOcrEngine(androidContext()) }

    single { PdfPageRenderer(androidContext()) }

    single { DocxTextExtractor(androidContext()) }

    single { RuleParser() }

    single { OpenRouterClient(createOpenRouterHttpClient()) }

    single<MedicalRepository> {
        MedicalRepositoryImpl(get(), get())
    }

    single<ResultsRepository> { ResultsRepositoryImpl() }

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
    viewModel { OcrAiScannerViewModel(get(), get<ExtractTextOfflineUseCase>(), get(), get(), get(), get(), get()) }
    viewModel { ResultsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel {
        OcrOfflineScannerViewModel(
            get<ExtractTextOfflineUseCase>(),
            get<CheckDuplicateUseCase>(),
            get<AddResultUseCase>(),
            get<RuleParser>(),
            get<PdfPageRenderer>()
        )
    }
}
