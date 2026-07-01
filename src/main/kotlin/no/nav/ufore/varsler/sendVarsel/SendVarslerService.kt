package no.nav.ufore.varsler.sendVarsel

import io.getunleash.Unleash
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.exporter.pushgateway.PushGateway
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SendVarslerService(
    private val unleash: Unleash,
    private val varselRepository: VarselRepository,
    private val varselProducer: SendVarselProducer,
    private val meterRegistry: MeterRegistry,
    @Value("\${app.pushgateway.address}") private val pushGatewayAddress: String,
    @Value("\${spring.application.name}") private val appName: String,
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
                meterRegistry.counter("ufore_varsler_bestilt_total").increment()
            } catch (e: Exception) {
                logger.error("Feil ved sending av varsel med id ${it.id} og varselId ${it.varselId}", e)
                meterRegistry.counter("ufore_varsler_bestilt_feil_total").increment()
            }
        }

        logger.info("Varsler sendt, avslutter")

        pushMetrikker()
    }

    private fun pushMetrikker() {
        try {
            val prometheusRegistry = meterRegistry as? PrometheusMeterRegistry
                ?: return logger.warn("MeterRegistry er ikke PrometheusMeterRegistry, kan ikke pushe metrikker")

            PushGateway.builder()
                .address(pushGatewayAddress)
                .job(appName)
                .registry(prometheusRegistry.prometheusRegistry)
                .build()
                .pushAdd()
        } catch (e: Exception) {
            logger.error("Klarte ikke å pushe metrikker til Pushgateway: ${e.message}")
        }
    }
}
