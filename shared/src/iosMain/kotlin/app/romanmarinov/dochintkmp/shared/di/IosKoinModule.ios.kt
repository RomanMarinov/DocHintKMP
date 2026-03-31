package app.romanmarinov.dochintkmp.shared.di

import app.romanmarinov.dochintkmp.data.local.DatabaseDriverFactory
import app.romanmarinov.dochintkmp.data.local.ProcessedHashStorage
import app.romanmarinov.dochintkmp.data.local.createProcessedHashStorage
import app.romanmarinov.dochintkmp.data.local.getProcessedHashesDataStorePath
import app.romanmarinov.dochintkmp.data.parser.RuleParser
import app.romanmarinov.dochintkmp.data.remote.DocumentsShareGateway
import app.romanmarinov.dochintkmp.data.remote.ShareNetworkConfig
import app.romanmarinov.dochintkmp.data.remote.createAppJson
import app.romanmarinov.dochintkmp.data.remote.createOpenRouterHttpClient
import app.romanmarinov.dochintkmp.data.remote.share.ShareRemoteDataSource
import app.romanmarinov.dochintkmp.data.remote.share.ShareRemoteDataSourceImpl
import app.romanmarinov.dochintkmp.data.repository.ResultsRepositoryImpl
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val iosDocumentsModule: Module = module {
    single { createOpenRouterHttpClient() }
    single { createAppJson() }
    single<ShareRemoteDataSource> {
        ShareRemoteDataSourceImpl(
            httpClient = get(),
            json = get(),
            baseUrl = ShareNetworkConfig.baseUrl
        )
    }
    single { DocumentsShareGateway(get()) }

    single { DatabaseDriverFactory() }
    single<ProcessedHashStorage> { createProcessedHashStorage(::getProcessedHashesDataStorePath) }
    single<ResultsRepository> { ResultsRepositoryImpl(get(), get()) }
    factory { ObserveResultsUseCase(get()) }
    factory { RemoveResultUseCase(get()) }
    factory { ClearResultsUseCase(get()) }
    single { RuleParser() }
    factory { CheckDuplicateUseCase(get()) }
    factory { AddResultUseCase(get()) }
}
