package app.romanmarinov.dochintkmp.data.remote.share

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument

interface ShareRemoteDataSource {
    suspend fun sendDocuments(items: List<HousingPaymentDocument>): ShareCreateResponse
    suspend fun receiveDocumentsByCode(code: String): List<HousingPaymentDocument>
}
