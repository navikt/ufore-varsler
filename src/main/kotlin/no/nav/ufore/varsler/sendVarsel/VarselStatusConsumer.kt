package no.nav.ufore.varsler.sendVarsel

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.varsel.action.Varseltype
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime
import java.util.UUID

@Component
class VarselStatusConsumer(
	private val objectMapper: ObjectMapper,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@KafkaListener(
		topics = ["\${app.kafka.min-side.varsel-hendelse}"],
		groupId = "\${app.kafka.min-side.consumer-group-id}",
	)
	fun consume(@Payload message: String) {
		val hendelse = objectMapper.readValue(message, VarselHendelse::class.java)
		if (hendelse.appnavn != "ufore-varsler") return

		logger.info("Mottok varselhendelse: {}", hendelse)
	}
}

data class VarselHendelse(
	@JsonProperty("@event_name")
	val eventName: EventName,
	val status: EksternStatus? = null,
	val varselType: Varseltype? = null,
	val varselId: String,
	val namespace: String,
	val appnavn: String,
	val tidspunkt: ZonedDateTime
)

enum class EventName {
	opprettet, inaktivert, slettet, eksternStatusOppdatert
}

enum class EksternStatus {
	feilet, info, bestilt, sendt, ferdigstilt, kansellert, venter
}
