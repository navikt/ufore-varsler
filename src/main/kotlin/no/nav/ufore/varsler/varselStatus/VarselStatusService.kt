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
    private val tokenService: TokenService,
    private val pidEncryptionClient: PidEncryptionClient,
    private val tilgangService: TilgangService,
) {

    fun hentStatus(token: String, fnr: String?, kryptertRepresentertFnr: String?): Boolean {
        val brukerType = tokenService.hentBrukertype(token)
        val gyldigToken = tokenService.sjekkGyldigToken(token)

        val fnr = when (brukerType) {
            Bruker.Borger -> {
                val innloggaBorgerFnr = gyldigToken.pid ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

                if (kryptertRepresentertFnr != null) {
                    val representertFnr = pidEncryptionClient.decrypt(kryptertRepresentertFnr)
                    tilgangService.sjekkRepresentantTilgang(token, innloggaBorgerFnr, representertFnr)
                    representertFnr
                } else {
                    tilgangService.sjekkBorgerTilgang(innloggaBorgerFnr, token)
                    innloggaBorgerFnr
                }
            }

            Bruker.Veileder -> {
                if (fnr == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
                val dekryptertFnr = pidEncryptionClient.decrypt(fnr)
                tilgangService.sjekkVeilederTilgang(token, dekryptertFnr)
                dekryptertFnr
            }
        }

        val varsel = varselRepository.hent(fnr, VarselType.UNGE_MED_UFORE) ?: return false
        return varsel.status != Status.OPPRETTET
    }
}
