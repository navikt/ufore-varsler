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

    // skal nå returnere ukryptert fnr
    fun sjekkRepresentantTilgang(token: String, innloggaBorgerFnr: String, kryptertRepresentertFnr: String): String {
        val representasjonsforhold = representasjonClient.hentRepresentasjon(token, innloggaBorgerFnr, kryptertRepresentertFnr)

        if (!representasjonsforhold.hasValidRepresentasjonsforhold) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        pdlClient.sjekkAdressebeskyttelse(representasjonsforhold.representertPid, token)

        return representasjonsforhold.representertPid
    }

    // TODO: Test med kode 6 bruker i miljø
    fun sjekkBorgerTilgang(innloggaBorgerFnr: String, token: String) {
        pdlClient.sjekkAdressebeskyttelse(innloggaBorgerFnr, token)
    }

    fun sjekkVeilederTilgang(token: String, fnr: String) {
        azureAdGrupperService.sjekkVeilederBasisTilganger(token)

        if (!azureAdGrupperService.harVeilederTilgangTilSkjermedeBorgere(token)) {
            val erBorgerSjermet = skjermingClient.erBorgerSkjermet(token, fnr)
            if (erBorgerSjermet) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        pdlClient.sjekkAdressebeskyttelse(fnr, token)
    }
}
