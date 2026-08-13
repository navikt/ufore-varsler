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
class TexasService(
    @Value("\${app.texas.token-introspection-endpoint}")
    private val introspectionEndpoint: String,
    @Value("\${app.texas.token-exchange-endpoint}")
    private val exchangeEndpoint: String,
) {

    private val logger = LoggerFactory.getLogger(TexasService::class.java)
    private val restClient = RestClient.create()

    fun sjekkGyldigToken(token: String): IntrospectionResponse {
        val brukerType = hentBrukertype(token)

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

    fun hentToken(token: String, target: String): String {
        val brukerType = hentBrukertype(token)
        val identityProvider = hentIdentityProvider(brukerType)

        val response = runCatching {
            restClient.post()
                .uri(exchangeEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExchangeRequest(token, target, identityProvider))
                .retrieve()
                .body<ExchangeResponse>()
        }.getOrNull()

        if (response == null) {
            logger.warn("Klarte ikke exchange token")
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return response.access_token
    }

    private fun hentIdentityProvider(brukerType: Bruker): String {
        return when (brukerType) {
            Bruker.Borger -> "tokenx"
            Bruker.Veileder -> "entra_id"
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



    private data class IntrospectionRequest(
        val identity_provider: String,
        val token: String,
    )

    data class IntrospectionResponse(
        val active: Boolean,
        val pid: String?,
        val error: String?,
    )

    private data class ExchangeRequest(
        val user_token: String,
        val target: String,
        val identity_provider: String,
    )

    private data class ExchangeResponse(
        val access_token: String,
        val expires_in: Int,
        val token_type: String,
    )


}

enum class Bruker { Veileder, Borger }