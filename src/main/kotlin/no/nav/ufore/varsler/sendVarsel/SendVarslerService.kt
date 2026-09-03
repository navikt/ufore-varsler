package no.nav.ufore.varsler.sendVarsel

import io.getunleash.Unleash
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SendVarslerService(
    private val unleash: Unleash,
    private val varselRepository: VarselRepository,
    private val varselProducer: SendVarselProducer,
) {

    companion object {
        val maksAntallPerDag = 300
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute() {
        logger.info("Starter jobb for å sende varsler")

        if (!unleash.isEnabled("ufore-varsler.send-varsler-jobb")) {
            logger.info("Jobb er slått av, avslutter")
            return
        }

        val antallBestiltIDag = varselRepository.antallBestiltIDag()
        if (antallBestiltIDag >= maksAntallPerDag) {
            logger.info("Dagens utsending er ferdig, avslutter")
            return
        }

        val antallSkalBestille = maksAntallPerDag - antallBestiltIDag
        val ikkeBestilte = varselRepository.hentOpprettet(antallSkalBestille)
        logger.info("Fant ${ikkeBestilte.size} ikke bestilte varsler, sender disse")

        ikkeBestilte.forEach {
            try {
                varselProducer.sendBeskjed(SendVarselRequest(it.varselId.toString(), it.mottakerFnr))
                varselRepository.oppdaterBestilt(it.varselId)
            } catch (e: Exception) {
                logger.error("Feil ved sending av varsel med id ${it.id} og varselId ${it.varselId}", e)
            }
        }

        logger.info("Varsler sendt, avslutter")
    }
}
