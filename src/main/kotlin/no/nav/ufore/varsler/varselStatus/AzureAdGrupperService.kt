package no.nav.ufore.varsler.varselStatus

import com.nimbusds.jwt.JWTParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AzureAdGrupperService(
    @Value("\${ad-grupper.skjermet}") private val skjermetGroupId: String,
    @Value("\${ad-grupper.brukerhjelpa}") private val brukerhjelpaGroupId: String,
    @Value("\${ad-grupper.okonomi}") private val okonomiGroupId: String,
    @Value("\${ad-grupper.saksbehandler}") private val saksbehandlerGroupId: String,
    @Value("\${ad-grupper.veileder}") private val veilederGroupId: String,
) {

    fun sjekkVeilederBasisTilganger(token: String) {
        val grupper = hentGrupper(token)
        val harMinstEnGyldigGruppe =
            grupper.any { it in listOf(saksbehandlerGroupId, veilederGroupId, brukerhjelpaGroupId, okonomiGroupId) }

        if (!harMinstEnGyldigGruppe) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }

    fun harVeilederTilgangTilSkjermedeBorgere(token: String): Boolean {
        val grupper = hentGrupper(token)
        return grupper.contains(skjermetGroupId)
    }

    private fun hentGrupper(token: String): List<String> =
        runCatching {
            JWTParser.parse(token).jwtClaimsSet.getStringListClaim("groups")
        }.getOrDefault(emptyList())
}