package app.romanmarinov.dochintkmp.data.remote

import app.romanmarinov.dochintkmp.data.remote.share.ShareCreateResponse
import app.romanmarinov.dochintkmp.data.remote.share.ShareRemoteDataSource
import app.romanmarinov.dochintkmp.domain.model.MedicalData

class DocumentsShareGateway(
    private val remoteDataSource: ShareRemoteDataSource
) {
    suspend fun sendDocuments(items: List<MedicalData>): ShareCreateResponse {
        return remoteDataSource.sendDocuments(items)
    }

    suspend fun receiveDocumentsByCode(code: String): List<MedicalData> {
        return remoteDataSource.receiveDocumentsByCode(code)
    }
}
