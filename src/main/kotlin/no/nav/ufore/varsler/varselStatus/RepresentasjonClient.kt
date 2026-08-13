package no.nav.ufore.varsler.varselStatus


import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody
import org.springframework.web.server.ResponseStatusException

@Component
class RepresentasjonClient(
    @Value("\${app.representasjon.url}") private val url: String,
    @Value("\${app.representasjon.target}") private val scope: String,
    private val tokenService: TokenService
) {
    private val restClient = RestClient.create()

    fun hentRepresentasjon(token: String, innloggaBorgerFnr: String, kryptertRepresentertFnr: String): RepresentasjonResponse {
        val accessToken = tokenService.hentToken(token, scope)
        return restClient
            .post()
            .uri("$url/representasjon/hasValidRepresentasjonsforhold")
            .header("Authorization", "Bearer $accessToken")
            .body(RepresenttasjonRequest(
                innloggaBorgerFnr,
                kryptertRepresentertFnr,
                representasjonstyper))
            .retrieve()
            .onStatus({ it == HttpStatus.FORBIDDEN}) { _, _ -> throw ResponseStatusException(HttpStatus.FORBIDDEN) }
            .requiredBody<RepresentasjonResponse>()
    }
}

data class RepresentasjonResponse (
    val hasValidRepresentasjonsforhold: Boolean,
)

data class RepresenttasjonRequest(
    val representertPid: String,
    val representantPid: String?,
    val validRepresentasjonstyper: List<String>,
    val includeRepresentertNavn: Boolean = false
)

private val representasjonstyper = listOf(
    "UFORETRYGD_LES",
    "UFORETRYGD_SKRIV",
    "VERGE_UFORETRYGD_LES",
    "VERGE_UFORETRYGD_SKRIV"
)


