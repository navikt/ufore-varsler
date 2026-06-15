package no.nav.ufore.varsler.sendVarsel

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.varsel.action.Varseltype
import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime

// https://navikt.github.io/tms-dokumentasjon/varsler/produsere/#overvaking-av-varsler
@Component
class MinSideVarselStatusConsumer(
    private val objectMapper: ObjectMapper,
    private val varselRepository: VarselRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

	@KafkaListener(
		topics = ["\${app.kafka.min-side.varsel-hendelse}"],
		groupId = "\${app.kafka.min-side.consumer-group-id}",
	)
	fun consume(@Payload message: String) {
		val minSideHendelse = objectMapper.readValue(message, MinSideVarselHendelse::class.java)
		if (minSideHendelse.appnavn != "ufore-varsler") return

        logger.info("Mottok varselhendelse fra Min side: {}", minSideHendelse)
        val varsel = varselRepository.hent(minSideHendelse.varselId)

        if (minSideHendelse.status == MinSideEksternStatus.sendt) {
            if (varsel?.status != Status.BESTILT) {
                throw FeilStatusException("Mottar varselId: ${minSideHendelse.varselId}, MinSideEksternStatus: ${minSideHendelse.status}. Feil status i database: ${varsel?.status}, prøver igjen")
            }
            varselRepository.oppdaterSendt(minSideHendelse.varselId)
        }

        if (minSideHendelse.eventName == MinSideEventName.inaktivert) {
            if (varsel?.status != Status.SENDT) {
                throw FeilStatusException("Mottar varselId: ${minSideHendelse.varselId}, eventName: ${minSideHendelse.eventName}. Feil status i database: ${varsel?.status}, prøver igjen")
            }
            varselRepository.oppdaterÅpnet(minSideHendelse.varselId)
        }

        if (minSideHendelse.status == MinSideEksternStatus.feilet) {
            logger.warn("Varsel med id ${minSideHendelse.varselId} feilet ved sending")// feilmelding
            varselRepository.oppdaterFeilet(minSideHendelse.varselId)
        }
	}
}

class FeilStatusException(message: String) : Exception(message)

data class MinSideVarselHendelse(
    @JsonProperty("@event_name")
	val eventName: MinSideEventName,
    val status: MinSideEksternStatus? = null,
    val varselType: Varseltype? = null,
    val varselId: String,
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
