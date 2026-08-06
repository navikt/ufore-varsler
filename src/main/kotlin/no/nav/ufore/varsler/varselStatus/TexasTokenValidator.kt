package no.nav.ufore.varsler.varselStatus

import com.nimbusds.jwt.JWTParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.server.ResponseStatusException

// Dokumentasjon: https://docs.nais.io/auth/tokenx/how-to/secure/#secure-your-api-with-tokenx
@Component
class TexasTokenValidator(
    @Value("\${app.texas.token-introspection-endpoint}")
    private val introspectionEndpoint: String,
) {

    private val logger = LoggerFactory.getLogger(TexasTokenValidator::class.java)
    private val restClient = RestClient.create()

    fun sjekkGyldigToken(token: String, brukerType: Bruker): IntrospectionResponse {
        val identityProvider = hentIdentityProvider(brukerType)

        val response = runCatching {
            restClient.post()
                .uri(introspectionEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(IntrospectionRequest(identityProvider, token))
                .retrieve()
                .body<IntrospectionResponse>()
        }.getOrNull()

        if (response == null || !response.active) {
            logger.warn("Ugyldig token, error: ${response?.error}")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }

        return response
    }

    fun hentIdentityProvider(brukerType: Bruker): String {
        return when (brukerType) {
            Bruker.Borger -> "tokenx"
            Bruker.Veileder -> "azuread"
        }
    }

    fun hentBrukertype(token: String): Bruker {
        val issuer = runCatching {
            JWTParser.parse(token).jwtClaimsSet.issuer
        }.getOrNull()

        return when {
            issuer?.contains("tokenx") == true -> Bruker.Borger
            issuer?.contains("microsoftonline") == true -> Bruker.Veileder
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
    }

    enum class Bruker { Veileder, Borger }

    private data class IntrospectionRequest(
        val identity_provider: String,
        val token: String,
    )

    data class IntrospectionResponse(
        val active: Boolean,
        val pid: String?,
        val error: String?,
    )
}
