package no.nav.ufore.varsler.opprettVarsel

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OpprettVarselConsumer(
    private val objectMapper: ObjectMapper,
    private val varselRepository: VarselRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.kafka.ufore-varsler.topic}"],
        groupId = "\${app.kafka.ufore-varsler.consumer-group-id}",
    )
    fun consume(@Payload message: String) {
        val melding = objectMapper.readValue(message, OpprettVarselMelding::class.java)

        if (varselRepository.hent(melding.fnr, melding.type) != null) {
            logger.info("Varsel med type ${melding.type} finnes allerede")
            return
        }

        val varsel = varselRepository.lagre(melding.fnr, melding.type)

        logger.info("Lagret varsel med id ${varsel.id}")

    }
}

data class OpprettVarselMelding(
    val fnr: String,
    val type: VarselType,
)