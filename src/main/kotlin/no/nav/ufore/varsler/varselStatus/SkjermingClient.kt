package no.nav.ufore.varsler.varselStatus

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody
import org.springframework.web.server.ResponseStatusException

@Component
class SkjermingClient(
    @Value("\${app.skjerming.url}") private val url: String,
    @Value("\${app.skjerming.target}") private val target: String,
    private val texasService: TexasService
) {

    private val restClient = RestClient.create()

    fun erBorgerSkjermet(token: String, fnr: String): Boolean {
        return texasService.hentToken(token, target).let { accessToken ->
            restClient
                .post()
                .uri("$url/skjermet")
                .header("Authorization", "Bearer $accessToken")
                .accept(MediaType.APPLICATION_JSON)
                .body(SkjermingRequest(fnr))
                .retrieve()
                .onStatus({ it == HttpStatus.FORBIDDEN}) { _, _ -> throw ResponseStatusException(HttpStatus.FORBIDDEN) }
                .requiredBody<Boolean>()
        }
    }

    data class SkjermingRequest(val personident: String)
}