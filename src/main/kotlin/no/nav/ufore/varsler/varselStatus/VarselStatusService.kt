package no.nav.ufore.varsler.varselStatus

import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import no.nav.ufore.varsler.opprettVarsel.VarselType
import org.springframework.stereotype.Service

@Service
class VarselStatusService(private val varselRepository: VarselRepository) {

    fun harMottattVarsel(fnr: String): Boolean =
        varselRepository.hent(fnr, VarselType.UNGE_MED_UFORE)?.status == Status.SENDT
}
