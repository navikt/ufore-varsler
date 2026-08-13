package no.nav.ufore.varsler.varselStatus.pdl

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.ufore.varsler.varselStatus.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody
import org.springframework.web.server.ResponseStatusException

@Component
class PdlClient(
    @Value("\${app.pdl.url}") private val url: String,
    @Value("\${app.pdl.target}") private val target: String,
    private val tokenService: TokenService
) {

    companion object {
        const val PDL_BEHANDLINGSNUMMER_VALUE = "B255"
        const val PDL_BEHANDLINGSNUMMER_KEY = "Behandlingsnummer"
    }

    private val logger = LoggerFactory.getLogger(PdlClient::class.java)
    private val restClient = RestClient.create()

    fun sjekkAdressebeskyttelse(fnr: String, token: String) {
        val query = PdlQueryBuilder.getAdressebeskyttelseQuery(fnr)
        val response = tokenService.hentToken(token, target).let {
            restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer $it")
                .header(PDL_BEHANDLINGSNUMMER_KEY, PDL_BEHANDLINGSNUMMER_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .retrieve()
                .requiredBody<PdlResponse>()
        }

        val beskyttelse = response.data.hentPerson?.adressebeskyttelse
        if (beskyttelse != null) {
            return

        } else {
            val error = response.errors?.firstOrNull() ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
            logger.warn("Feil ved henting av adressebeskyttelse fra PDL, code: ${error.extensions.code}, error: ${error.message}")

            when (error.extensions.code) {
                PdlErrorCode.UNAUTHENTICATED, PdlErrorCode.UNAUTHORIZED -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
                else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND, // forvaltningsloven paragraf 19
    STRENGT_FORTROLIG,        // kode 6
    FORTROLIG,                // kode 7
    UGRADERT
}

private data class PdlResponse(
    val data: PdlData,
    val errors: List<PdlError>?
)

private data class PdlData(
    val hentPerson: PdlHentPerson?
)

private data class PdlHentPerson(
    val adressebeskyttelse: List<PdlAdressebeskyttelse>
)

private data class PdlAdressebeskyttelse(
    val gradering: AdressebeskyttelseGradering
)

private data class PdlError(
    val message: String,
    val extensions: PdlExtensions
)

private data class PdlExtensions(
    val code: PdlErrorCode,
)

private enum class PdlErrorCode {
    @JsonProperty("unauthenticated") UNAUTHENTICATED,
    @JsonProperty("unauthorized") UNAUTHORIZED,
    @JsonProperty("not_found") NOT_FOUND,
    @JsonProperty("bad_request") BAD_REQUEST,
    @JsonProperty("server_error") SERVER_ERROR
}