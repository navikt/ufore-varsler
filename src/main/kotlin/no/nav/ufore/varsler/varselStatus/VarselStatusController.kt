package no.nav.ufore.varsler.varselStatus

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/varsler")
class VarselStatusController(
    private val varselStatusService: VarselStatusService,
    private val tokenValidator: TexasService,
) {

    @PostMapping("/status")
    fun hentStatus(@RequestHeader("Authorization", required = false) authHeader: String?, @RequestBody requestBody: VarselStatusRequest?): ResponseEntity<VarselStatusResponse> {
        val token = authHeader?.removePrefix("Bearer ") ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val brukerType = tokenValidator.hentBrukertype(token)

        val gyldigToken = tokenValidator.sjekkGyldigToken(token, brukerType)

        if (brukerType == Bruker.Veileder) {

        }


        /*
        * ufore-varsler

hvis borger
    hvis nav-obo-cookie
        sjekk hasValidRepresentasjonsforhold
        sjekk adressebeskyttelse
    hvis ikke cookie
        Hvis innlogget bruker har adressebeskyttelse (STRENGT_FORTROLIG eller STRENGT_FORTROLIG_UTLAND). Må logge inn med høyt innloggingsnivå (feks Bank ID)

hvis veileder
    dekrypter pid fra request
    sjekk at "groups" i token har mist en av nødvendige grupper (basistilgang)
    sjekk borger er skjermet og veileder kan se på skjerma borger
    sjekk adressebeskyttelse på borger og om veileder har tilgang til den typen adressebeskyttelse
        *
        * */

        val fnr = when (brukerType) {
            Bruker.Borger -> gyldigToken.pid
            Bruker.Veileder -> requestBody?.pid
        } ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(VarselStatusResponse(varselStatusService.harMottattVarsel(fnr)))
    }
}

data class VarselStatusResponse(val harMottattVarsel: Boolean)

data class VarselStatusRequest(val pid: String)
