package no.nav.ufore.varsler.varselStatus

import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import no.nav.ufore.varsler.opprettVarsel.VarselType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class VarselStatusService(
    private val varselRepository: VarselRepository,
    private val texasService: TexasService,
    private val pidEncryptionClient: PidEncryptionClient,
    private val tilgangService: TilgangService,
) {

    fun hentStatus(token: String, fnr: String?, kryptertRepresentertFnr: String?): Boolean {
        val brukerType = texasService.hentBrukertype(token)
        val gyldigToken = texasService.sjekkGyldigToken(token)

        val fnr = when (brukerType) {
            Bruker.Borger -> {
                val innloggaBorgerFnr = gyldigToken.pid ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

                if (kryptertRepresentertFnr != null) {
                    val representertFnr = pidEncryptionClient.decrypt(kryptertRepresentertFnr, token)
                    tilgangService.sjekkRepresentantTilgang(token, innloggaBorgerFnr, representertFnr)
                    representertFnr
                } else {
                    tilgangService.sjekkBorgerTilgang(innloggaBorgerFnr, token)
                    innloggaBorgerFnr
                }
            }

            Bruker.Veileder -> {
                if (fnr == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
                val dekryptertFnr = pidEncryptionClient.decrypt(fnr, token)
                tilgangService.sjekkVeilederTilgang(token, dekryptertFnr)
                dekryptertFnr
            }
        }


        return varselRepository.hent(fnr, VarselType.UNGE_MED_UFORE)?.status != Status.OPPRETTET
    }

}
