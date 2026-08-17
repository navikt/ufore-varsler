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
    private val representasjonClient: RepresentasjonClient,
){

    fun sjekkRepresentantTilgang(token: String, innloggaBorgerFnr: String, kryptertRepresentertFnr: String): String {
        val representasjonsforhold = representasjonClient.hentRepresentasjon(token, innloggaBorgerFnr, kryptertRepresentertFnr)

        if (!representasjonsforhold.hasValidRepresentasjonsforhold) throw IkkeTilgangException("tilgangservice", "Har ikke gyldig representasjonsforhold")

        pdlClient.sjekkAdressebeskyttelse(representasjonsforhold.representertPid, token)

        return representasjonsforhold.representertPid
    }

    fun sjekkBorgerTilgang(innloggaBorgerFnr: String, token: String) {
        pdlClient.sjekkAdressebeskyttelse(innloggaBorgerFnr, token)
    }

    fun sjekkVeilederTilgang(token: String, fnr: String) {
        azureAdGrupperService.sjekkVeilederBasisTilganger(token)

        if (!azureAdGrupperService.harVeilederTilgangTilSkjermedeBorgere(token)) {
            val erBorgerSjermet = skjermingClient.erBorgerSkjermet(token, fnr)
            if (erBorgerSjermet) throw IkkeTilgangException("tilgangservice", "Veileder har ikke tilgang til borger")
        }

        pdlClient.sjekkAdressebeskyttelse(fnr, token)
    }
}
