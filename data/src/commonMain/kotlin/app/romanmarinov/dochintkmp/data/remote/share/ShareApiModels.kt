package app.romanmarinov.dochintkmp.data.remote.share

import app.romanmarinov.dochintkmp.domain.model.MedicalData
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
    @SerialName("medical_document_export")
    MEDICAL_DOCUMENT_EXPORT
}

@Serializable
data class ShareItem(
    val kind: String = "medical_analysis",
    val payload: MedicalData
)

@Serializable
data class ShareCreateResponse(
    val code: String,
    val expiresAt: String
)
