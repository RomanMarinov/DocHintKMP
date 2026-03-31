package app.romanmarinov.dochintkmp.data.remote.share

import app.romanmarinov.dochintkmp.domain.model.MedicalData

interface ShareRemoteDataSource {
    suspend fun sendDocuments(items: List<MedicalData>): ShareCreateResponse
    suspend fun receiveDocumentsByCode(code: String): List<MedicalData>
}
