package app.romanmarinov.dochintkmp.data.remote.share

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShareEnvelope(
    val schemaVersion: Int,
    val messageType: ShareMessageType,
    val items: List<ShareItem>
)

@Serializable
enum class ShareMessageType {
    @SerialName("housing_document_export")
    HOUSING_DOCUMENT_EXPORT
}

@Serializable
data class ShareItem(
    val kind: String = "housing_bill",
    val payload: HousingPaymentDocument
)

@Serializable
data class ShareCreateResponse(
    val code: String,
    val expiresAt: String
)
