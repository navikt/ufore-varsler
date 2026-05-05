package no.nav.ufore.varsler

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class VarselConsumer(
	private val objectMapper: ObjectMapper,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@KafkaListener(
		topics = ["\${app.kafka.consumer-topic}"],
		groupId = "\${app.kafka.consumer-group-id}",
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
	val varselType: String,
	val eventId: String,
	val namespace: String,
	val appnavn: String,
)
