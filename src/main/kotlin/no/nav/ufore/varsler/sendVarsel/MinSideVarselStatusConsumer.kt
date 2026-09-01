package no.nav.ufore.varsler.sendVarsel

import com.fasterxml.jackson.annotation.JsonProperty
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tms.varsel.action.Varseltype
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
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
        logger.info("Mottok varselhendelse fra Min side: varselId: ${minSideHendelse.varselId}, status: ${minSideHendelse.status}, eventName: ${minSideHendelse.eventName} appnavn: ${minSideHendelse.appnavn}")

        val varselId =
            runCatching { UUID.fromString(minSideHendelse.varselId) }
                .getOrElse {
                    logger.info("Ugyldig UUID i varselId, melding tilhører ikke vår app, fortsetter")
                    return
                }
        val varsel = varselRepository.hent(varselId)
        if (varsel == null) {
            logger.info("Varsel med varselId $varselId ikke funnet i databasen, fortsetter")
            return
        }

        if (minSideHendelse.status == MinSideEksternStatus.sendt) {
            varselRepository.oppdaterSendt(varselId, minSideHendelse.tidspunkt)
            meterRegistry.counter("ufore_varsler_status_total", "status", "sendt").increment()
        }

        if (minSideHendelse.eventName == MinSideEventName.inaktivert) {
            varselRepository.oppdaterÅpnet(varselId)
            meterRegistry.counter("ufore_varsler_status_total", "status", "aapnet").increment()
        }

        if (minSideHendelse.status == MinSideEksternStatus.feilet) {
            logger.warn("Varsel med id $varselId feilet ved sending, feilmelding: ${minSideHendelse.feilmelding}")
            varselRepository.oppdaterFeilet(varselId)
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
    val varselId: String, // Min side operer med både UUID og ULID
    val namespace: String,
    val appnavn: String,
    val tidspunkt: OffsetDateTime,
    val feilmelding: String? = null,
)

enum class MinSideEventName {
	opprettet, inaktivert, slettet, eksternStatusOppdatert
}

enum class MinSideEksternStatus {
	feilet, info, bestilt, sendt, ferdigstilt, kansellert, venter
}
