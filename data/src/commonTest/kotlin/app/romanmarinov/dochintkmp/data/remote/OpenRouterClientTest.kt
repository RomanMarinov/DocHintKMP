package app.romanmarinov.dochintkmp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenRouterClientTest {

    @Test
    fun parseWithLlm_parsesSuccessfulResponse_andNormalizesDocumentType() = runBlocking {
        val client = clientWithResponse(
            """
            {
              "choices":[{"message":{"content":"{\"document_type\":\"Квитанция ЖКХ\",\"institution\":\"ООО УК\",\"document_date\":\"2024-10\",\"service_lines\":[{\"name\":\"Отопление\",\"amount_to_pay\":\"2125\"}]}"}}],
              "usage":{"prompt_tokens":11,"completion_tokens":22,"total_tokens":33}
            }
            """.trimIndent()
        )
        val sut = OpenRouterClient(client)

        val result = sut.parseWithLlm("test-key", "clean text")

        assertEquals("Квитанция ЖКУ", result.housingDocument.documentType)
        assertEquals("ООО УК", result.housingDocument.institution)
        assertEquals(33, result.totalTokens)
        assertEquals(OpenRouterClient.MODEL_TEXT, result.model)
        assertEquals("Отопление", result.housingDocument.serviceLines?.first()?.name)
    }

    @Test
    fun parseWithLlm_throwsFriendlyMessage_forUnauthorized() = runBlocking {
        val client = clientWithStatus(HttpStatusCode.Unauthorized, """{"error":"unauthorized"}""")
        val sut = OpenRouterClient(client)

        val ex = assertFailsWith<IllegalStateException> {
            sut.parseWithLlm("bad-key", "text")
        }

        assertTrue(ex.message.orEmpty().contains("401"))
    }

    @Test
    fun parseWithLlm_throwsWhenModelReturnsEmptyChoices() = runBlocking {
        val client = clientWithResponse("""{"choices":[],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}""")
        val sut = OpenRouterClient(client)

        val ex = assertFailsWith<IllegalStateException> {
            sut.parseWithLlm("key", "text")
        }

        assertTrue(ex.message.orEmpty().contains("пустой ответ"))
    }

    @Test
    fun parseWithLlm_throwsForMedicalLookingDocument() = runBlocking {
        val client = clientWithResponse(
            """
            {"choices":[{"message":{"content":"{\"document_type\":\"ОАК\",\"service_lines\":[{\"name\":\"Гемоглобин\",\"amount_to_pay\":\"1\"}]}"}}]}
            """.trimIndent()
        )
        val sut = OpenRouterClient(client)

        val ex = assertFailsWith<IllegalStateException> {
            sut.parseWithLlm("key", "text")
        }

        val msg = ex.message.orEmpty()
        assertTrue(msg.contains("медицинский") || msg.contains("квитац") || msg.contains("ЖКХ"))
    }

    @Test
    fun getKeyInfo_returnsParsedData_forValidResponse() = runBlocking {
        val client = clientWithResponse(
            """
            {"data":{"label":"my key","usage":12.5,"limit_remaining":87.5,"is_free_tier":false}}
            """.trimIndent()
        )
        val sut = OpenRouterClient(client)

        val info = sut.getKeyInfo("k")

        assertEquals("my key", info?.label)
        assertEquals(12.5, info?.usage)
    }

    @Test
    fun getKeyInfo_returnsNull_whenRequestFails() = runBlocking {
        val client = clientWithStatus(HttpStatusCode.InternalServerError, """{"error":"boom"}""")
        val sut = OpenRouterClient(client)

        val info = sut.getKeyInfo("k")

        assertNull(info)
    }

    private fun clientWithResponse(jsonBody: String): HttpClient {
        val engine = MockEngine {
            respond(
                content = jsonBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private fun clientWithStatus(status: HttpStatusCode, body: String): HttpClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }
}
