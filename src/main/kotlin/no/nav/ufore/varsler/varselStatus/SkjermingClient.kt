package no.nav.ufore.varsler.varselStatus

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody

@Component
class SkjermingClient(
    @Value("\${app.skjerming.url}") private val baseUrl: String,
    @Value("\${app.skjerming.target}") private val target: String,
    private val tokenService: TokenService
) {

    private val restClient = RestClient.create()

    fun erBorgerSkjermet(token: String, fnr: String): Boolean {
        val url = "$baseUrl/skjermet"
        return tokenService.hentToken(token, target).let { accessToken ->
            restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer $accessToken")
                .accept(MediaType.APPLICATION_JSON)
                .body(SkjermingRequest(fnr))
                .retrieve()
                .onStatus({ it == HttpStatus.FORBIDDEN}) { _, response -> throw IkkeTilgangException(url, "${response.statusCode} - ${response.statusText}") }
                .requiredBody<Boolean>()
        }
    }

    data class SkjermingRequest(val personident: String)
}