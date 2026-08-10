package no.nav.ufore.varsler.varselStatus

import no.nav.pensjon.selvbetjening.inntektsplanleggerenbackend.configuration.withMdcContext
import no.nav.pensjon.selvbetjening.inntektsplanleggerenbackend.util.NAV_CALL_ID_HEADER
import no.nav.pensjon.selvbetjening.inntektsplanleggerenbackend.util.getCurrentCallId
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PidEncryptionClient(
    @Value("\${app.pid-encryption.target}") private val target: String,
    @Value("\${app.pid-encryption.url}") private val baseUrl: String,

    private val azureAdService: AzureAdService,
    private val webClient: WebClient,
    private val texasService: TexasService,
) {

//        - name: PID_ENCRYPTION_SCOPE
//      value: api://prod-gcp.pensjon-person.pensjon-pid-encryption/.default

    //     - name: PID_ENCRYPTION_SCOPE
    //      value: api://dev-gcp.pensjon-person.pensjon-pid-encryption/.default
    fun decrypt(encryptedPid: String, token: String): String? =
        texasService.hentToken(token, target,  ).let { token ->
            webClient
                .post()
                .uri("$baseUrl/api/decrypt")
                .header("Authorization", "Bearer $token")
                .header(NAV_CALL_ID_HEADER, getCurrentCallId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(encryptedPid)
                .retrieve()
                .bodyToMono(String::class.java)
                .withMdcContext()
                .block()
        }
}
