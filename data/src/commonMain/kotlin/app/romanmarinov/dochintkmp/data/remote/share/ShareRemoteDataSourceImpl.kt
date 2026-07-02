package app.romanmarinov.dochintkmp.data.remote.share

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ShareRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String
) : ShareRemoteDataSource {

    override suspend fun sendDocuments(items: List<HousingPaymentDocument>): ShareCreateResponse {
        val envelope = ShareEnvelope(
            schemaVersion = 1,
            messageType = ShareMessageType.HOUSING_DOCUMENT_EXPORT,
            items = items.map { ShareItem(payload = it.copy(source = null)) }
        )

        val response = httpClient.post("$baseUrl/${ShareApiEndpoint.SHARE}") {
            contentType(ContentType.Application.Json)
            setBody(envelope)
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Share create failed: ${response.status.value}, body=$bodyText")
        }
        return json.decodeFromString(bodyText)
    }

    override suspend fun receiveDocumentsByCode(code: String): List<HousingPaymentDocument> {
        val trimmed = code.trim()
        val response = httpClient.get("$baseUrl/${ShareApiEndpoint.consumeShare(trimmed)}")
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Share consume failed: ${response.status.value}, body=$bodyText")
        }
        val envelope = json.decodeFromString<ShareEnvelope>(bodyText)
        return envelope.items.map { it.payload.copy(source = null) }
    }
}
