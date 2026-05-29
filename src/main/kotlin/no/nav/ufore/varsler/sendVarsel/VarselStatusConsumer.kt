package no.nav.ufore.varsler.sendVarsel

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime

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
	val eventName: String,
	val varselType: String? = null,
	val varselId: String,
	val namespace: String,
	val appnavn: String,
    val tidspunkt: ZonedDateTime
)
