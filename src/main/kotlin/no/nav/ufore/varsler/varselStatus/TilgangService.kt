package no.nav.ufore.varsler.varselStatus

import no.nav.ufore.varsler.varselStatus.pdl.PdlClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class TilgangService (
    private val azureAdGrupperService: AzureAdGrupperService,
    private val skjermingClient: SkjermingClient,
    private val pdlClient: PdlClient,
){

//    hvis borger
//      hvis nav-obo-cookie
//          sjekk hasValidRepresentasjonsforhold
//          sjekk adressebeskyttelse
//      hvis ikke cookie
//          Hvis innlogget bruker har adressebeskyttelse (STRENGT_FORTROLIG eller STRENGT_FORTROLIG_UTLAND). Må logge inn med høyt innloggingsnivå (feks Bank ID)
    fun sjekkBorgerTilgang(token: String) {

    }

    fun sjekkVeilederTilgang(token: String, fnr: String) {
        azureAdGrupperService.sjekkVeilederBasisTilganger(token)

        if (!azureAdGrupperService.harVeilederTilgangTilSkjermedeBorgere(token)) {
            val erBorgerSjermet = skjermingClient.erBorgerSkjermet(token, fnr)
            if (erBorgerSjermet == null || erBorgerSjermet) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        pdlClient.sjekkAdressebeskyttelse(fnr, token)
    }
}