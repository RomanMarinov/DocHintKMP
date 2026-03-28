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
              "choices":[{"message":{"content":"{\"document_type\":\"oak\",\"institution\":\"Invitro\",\"doctor_name\":\"Иванов\",\"analysis_date\":\"12.03.2025\",\"indicators\":[{\"name\":\"Гемоглобин\",\"value\":\"120\",\"reference_range\":\"120-160\"}]}"}}],
              "usage":{"prompt_tokens":11,"completion_tokens":22,"total_tokens":33}
            }
            """.trimIndent()
        )
        val sut = OpenRouterClient(client)

        val result = sut.parseWithLlm("test-key", "clean text")

        assertEquals("ОАК", result.data.documentType)
        assertEquals("Invitro", result.data.institution)
        assertEquals(33, result.totalTokens)
        assertEquals(OpenRouterClient.MODEL_TEXT, result.model)
        assertEquals("Гемоглобин", result.data.indicators?.first()?.name)
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
    fun parseWithLlm_throwsUnsupported_forNonBloodDocumentType() = runBlocking {
        val client = clientWithResponse(
            """
            {"choices":[{"message":{"content":"{\"document_type\":\"Анализ мочи\",\"indicators\":[{\"name\":\"Белок\",\"value\":\"1\"}]}"}}]}
            """.trimIndent()
        )
        val sut = OpenRouterClient(client)

        val ex = assertFailsWith<IllegalStateException> {
            sut.parseWithLlm("key", "text")
        }

        assertTrue(ex.message.orEmpty().contains("не поддерживается"))
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
