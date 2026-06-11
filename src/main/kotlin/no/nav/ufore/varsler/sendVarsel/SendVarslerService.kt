package no.nav.ufore.varsler.sendVarsel

import io.getunleash.Unleash
import no.nav.ufore.varsler.opprettVarsel.Status
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
    private val varselTekst = "Du har fått et varsel fra Nav. Logg inn på nav.no for å se mer."

    fun execute() {
        logger.info("Starter jobb for å sende varsler")

        if (!unleash.isEnabled("ufore-varsler.send-varsler-jobb")) {
            logger.info("Jobb er slått av, avslutter")
            return
        }

        val antallSendtIDag = varselRepository.antallSendtIDag()
        if (antallSendtIDag >= maksAntallPerDag) {
            logger.info("Dagens utsending er ferdig, avslutter")
            return
        }

        val antallSkalSende = maksAntallPerDag - antallSendtIDag
        val ikkeSendte = varselRepository.hentIkkeSendte(antallSkalSende)
        logger.info("Fant ${ikkeSendte.size} ikke sendte varsler, sender disse")

        ikkeSendte.forEach {
            try {
                varselProducer.sendBeskjed(SendVarselRequest(it.varselId.toString(), it.mottakerFnr, varselTekst))
                varselRepository.oppdaterSendt(it.id.toString())
            } catch (e: Exception) {
                logger.error("Feil ved sending av varsel med id ${it.id} og varselId ${it.varselId}", e)
            }
        }

        logger.info("Varsler sendt, avslutter")
    }
}
