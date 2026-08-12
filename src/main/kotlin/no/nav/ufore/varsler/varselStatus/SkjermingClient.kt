package no.nav.ufore.varsler.varselStatus

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SkjermingClient(
    @Value("\${app.skjerming.url}") private val url: String,
    @Value("\${app.skjerming.target}") private val target: String,
    private val texasService: TexasService
) {

    private val restClient = RestClient.create()

    fun erBorgerSkjermet(token: String, fnr: String): Boolean? {
        return texasService.hentToken(token, target).let { accessToken ->
            restClient
                .post()
                .uri("$url/skjermet")
                .header("Authorization", "Bearer $accessToken")
                .accept(MediaType.APPLICATION_JSON)
                .body(SkjermingRequest(fnr))
                .retrieve()
                .body<Boolean>()
        }
    }

    data class SkjermingRequest(val personident: String)
}