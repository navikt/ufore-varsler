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
                    tilgangService.sjekkRepresentantTilgang(token, innloggaBorgerFnr, kryptertRepresentertFnr)

                }

                // Representant
                // sjekke om kryptret fnr -> dekrypter fnr -> tilgangService.sjekkRepresentasjonTilgang




                // TODO, håndtere fnr for representert borger, hvis representasjon finnes, vanlig gyldig token hvis ikke

                innloggaBorgerFnr
            }
            Bruker.Veileder -> {
                if (fnr == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
                val dekryptertFnr = pidEncryptionClient.decrypt(fnr, token) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
                tilgangService.sjekkVeilederTilgang(token, dekryptertFnr)
                dekryptertFnr
            }
        }


        return varselRepository.hent(fnr, VarselType.UNGE_MED_UFORE)?.status != Status.OPPRETTET
    }

}
