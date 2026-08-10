package no.nav.ufore.varsler.varselStatus

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PidEncryptionClient(
    @Value("\${app.pid-encryption.target}") private val target: String,
    @Value("\${app.pid-encryption.url}") private val baseUrl: String,

    private val texasService: TexasService,
) {
    private val restClient = RestClient.create()

    fun decrypt(encryptedPid: String, token: String): String? =
        texasService.hentToken(token, target,  ).let { token ->
            restClient
                .post()
                .uri("$baseUrl/api/decrypt")
                .header("Authorization", "Bearer $token")
//                .header(NAV_CALL_ID_HEADER, getCurrentCallId()) Skal vi implementere kall id og MDC?
                .accept(MediaType.APPLICATION_JSON)
                .body(encryptedPid)
                .retrieve()
                .body<String>(String::class.java)
//                .withMdcContext()
        }
}
