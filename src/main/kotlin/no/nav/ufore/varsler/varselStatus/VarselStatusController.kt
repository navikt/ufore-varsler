package no.nav.ufore.varsler.varselStatus

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/varsler")
class VarselStatusController(
    private val varselStatusService: VarselStatusService,
    private val tokenValidator: TexasService,
    private val pidEncryptionClient: PidEncryptionClient,
    private val azureAdGrupperService: AzureAdGrupperService,
    private val skjermingClient: SkjermingClient,
) {

    private val logger = LoggerFactory.getLogger(VarselStatusController::class.java)

    @PostMapping("/status")
    fun hentStatus(@RequestHeader("Authorization", required = false) authHeader: String?, @RequestBody requestBody: VarselStatusRequest?): ResponseEntity<VarselStatusResponse> {
        val token = authHeader?.removePrefix("Bearer ") ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val brukerType = tokenValidator.hentBrukertype(token)
        val gyldigToken = tokenValidator.sjekkGyldigToken(token)


        /*
            hvis veileder
            - dekrypter pid fra request
            - sjekk at "groups" i token har mist en av nødvendige grupper (basistilgang)
            - sjekk borger er skjermet og veileder kan se på skjerma borger
            - sjekk adressebeskyttelse på borger og om veileder har tilgang til den typen adressebeskyttelse
        */
        if (brukerType == Bruker.Veileder) {
            if (requestBody?.pid == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
            val pid = pidEncryptionClient.decrypt(requestBody.pid, token) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

            azureAdGrupperService.sjekkVeilederBasisTilganger(token)

            if (!azureAdGrupperService.harVeilederTilgangTilSkjermedeBorgere(token)) {
                skjermingClient.erBorgerSkjermet(token, pid) ?: throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }


        }

        /*
        * ufore-varsler

hvis borger
    hvis nav-obo-cookie
        sjekk hasValidRepresentasjonsforhold
        sjekk adressebeskyttelse
    hvis ikke cookie
        Hvis innlogget bruker har adressebeskyttelse (STRENGT_FORTROLIG eller STRENGT_FORTROLIG_UTLAND). Må logge inn med høyt innloggingsnivå (feks Bank ID)

*/

        val fnr = when (brukerType) {
            Bruker.Borger -> gyldigToken.pid
            Bruker.Veileder -> requestBody?.pid
        } ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(VarselStatusResponse(varselStatusService.harMottattVarsel(fnr)))
    }
}

data class VarselStatusResponse(val harMottattVarsel: Boolean)

data class VarselStatusRequest(val pid: String)
