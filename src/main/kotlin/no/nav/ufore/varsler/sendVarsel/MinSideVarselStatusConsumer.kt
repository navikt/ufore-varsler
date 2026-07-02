package no.nav.ufore.varsler.sendVarsel

import com.fasterxml.jackson.annotation.JsonProperty
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tms.varsel.action.Varseltype
import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime
import java.util.UUID

// https://navikt.github.io/tms-dokumentasjon/varsler/produsere/#overvaking-av-varsler
@Component
class MinSideVarselStatusConsumer(
    private val objectMapper: ObjectMapper,
    private val varselRepository: VarselRepository,
    private val meterRegistry: MeterRegistry,
    @Value("\${spring.application.name}") private val appName: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

	@KafkaListener(
		topics = ["\${app.kafka.min-side.varsel-hendelse}"],
		groupId = "\${app.kafka.min-side.consumer-group-id}",
	)
	fun consume(@Payload message: String) {
		val minSideHendelse = objectMapper.readValue(message, MinSideVarselHendelse::class.java)
		if (minSideHendelse.appnavn != appName) return

        // TODO: Nå kaster vi ikke lenger feil når varsler har feil status. Da kan vi miste en overgang.
        // Spørsmålet er hvor viktig det er? Med denne løsningen lagrer vi alltid "mottatt", men kan miste "sendt"
        // Når vi kasta feil ble partisjonen helt blokka i det uendelige
        logger.info("Mottok varselhendelse fra Min side: {}", minSideHendelse)
        val varsel = varselRepository.hent(minSideHendelse.varselId)

        if (minSideHendelse.status == MinSideEksternStatus.sendt) {
            if (varsel?.status != Status.BESTILT) {
                meterRegistry.counter("ufore_varsler_status_feil_total", "aarsak", "feil_rekkefolge").increment()
                logger.warn("Mottok sendt-hendelse for varselId=${minSideHendelse.varselId} men status i DB er ${varsel?.status} — hopper over")
                return
            }
            varselRepository.oppdaterSendt(minSideHendelse.varselId)
            meterRegistry.counter("ufore_varsler_status_total", "status", "sendt").increment()
        }

        if (minSideHendelse.eventName == MinSideEventName.inaktivert) {
            if (varsel == null) {
                logger.warn("Mottok inaktivert-hendelse for ukjent varselId=${minSideHendelse.varselId} — hopper over")
                return
            }
            varselRepository.oppdaterÅpnet(minSideHendelse.varselId)
            meterRegistry.counter("ufore_varsler_status_total", "status", "aapnet").increment()
        }

        if (minSideHendelse.status == MinSideEksternStatus.feilet) {
            logger.warn("Varsel med id ${minSideHendelse.varselId} feilet ved sending")// feilmelding
            varselRepository.oppdaterFeilet(minSideHendelse.varselId)
            meterRegistry.counter("ufore_varsler_status_total", "status", "feilet").increment()
        }
	}
}

class FeilStatusException(message: String) : Exception(message)

data class MinSideVarselHendelse(
    @JsonProperty("@event_name")
	val eventName: MinSideEventName,
    val status: MinSideEksternStatus? = null,
    val varselType: Varseltype? = null,
    val varselId: UUID,
    val namespace: String,
    val appnavn: String,
    val tidspunkt: ZonedDateTime
)

enum class MinSideEventName {
	opprettet, inaktivert, slettet, eksternStatusOppdatert
}

enum class MinSideEksternStatus {
	feilet, info, bestilt, sendt, ferdigstilt, kansellert, venter
}
