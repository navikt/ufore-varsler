package no.nav.ufore.varsler.sendVarsel

import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class SendVarselProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	@Value("\${app.kafka.min-side.brukervarsel}") private val producerTopic: String,
	@Value("\${NAIS_CLUSTER_NAME:local}") private val cluster: String,
	@Value("\${NAIS_NAMESPACE:local}") private val namespace: String,
	@Value("\${NAIS_APP_NAME:\${spring.application.name}}") private val appnavn: String
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	fun sendBeskjed(request: SendBeskjedRequest): SendResult {
		val ident = request.ident.trim()
		val tekst = request.tekst.trim()

		if (!ident.matches(Regex("\\d{11}"))) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ident må bestå av 11 siffer")
		}

		if (tekst.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tekst kan ikke være tom")
		}

		val varselId = UUID.randomUUID().toString()
		val melding = VarselActionBuilder.opprett {
			this.type = Varseltype.Beskjed
			this.link = "https://www.nav.no/uforetrygd/selvbetjening"
			this.varselId = varselId
			this.ident = ident
			this.sensitivitet = Sensitivitet.Substantial
			this.tekst = Tekst(
				spraakkode = "nb",
				tekst = tekst,
				default = true,
			)
			this.produsent = Produsent(cluster, namespace, appnavn)
		}

		kafkaTemplate.send(producerTopic, varselId, melding).get(10, TimeUnit.SECONDS)
		// TODO: Lagre i database at varsel er sendt

		logger.info("Sendte varsel med id=$varselId")

		return SendResult(varselId)
	}
}

data class SendResult(val varselId: String)
