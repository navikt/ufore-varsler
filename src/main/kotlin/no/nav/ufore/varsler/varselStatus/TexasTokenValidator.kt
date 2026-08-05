package no.nav.ufore.varsler.varselStatus

import com.nimbusds.jwt.JWTParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

// Dokumentasjon: https://docs.nais.io/auth/tokenx/how-to/secure/#secure-your-api-with-tokenx
@Component
class TexasTokenValidator(
    @Value("\${app.texas.token-introspection-endpoint}")
    private val introspectionEndpoint: String,
) {

    private val logger = LoggerFactory.getLogger(TexasTokenValidator::class.java)
    private val restClient = RestClient.create()

    fun hentFnr(authHeader: String): String? {
        val token = authHeader.removePrefix("Bearer ")

        val issuer = runCatching {
            JWTParser.parse(token).jwtClaimsSet.issuer
        }.getOrNull()

        // TODO: Sjekk issuer

        val response = runCatching {
            restClient.post()
                .uri(introspectionEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(IntrospectionRequest("tokenx", token))
                .retrieve()
                .body<IntrospectionResponse>()
        }.getOrNull()

        if (response == null || !response.active) {
            logger.warn("Ugyldig token, error: ${response?.error}")
            return null
        }

        return response.pid
    }
}

private data class IntrospectionRequest(
    val identity_provider: String,
    val token: String,
)

private data class IntrospectionResponse(
    val active: Boolean,
    val pid: String?,
    val error: String?,
)
