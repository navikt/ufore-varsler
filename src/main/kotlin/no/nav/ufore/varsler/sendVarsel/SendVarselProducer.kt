package no.nav.ufore.varsler.sendVarsel

import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@Service
class SendVarselProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	@Value("\${app.kafka.min-side.brukervarsel}") private val producerTopic: String,
	@Value("\${NAIS_CLUSTER_NAME:local}") private val cluster: String,
	@Value("\${NAIS_NAMESPACE:local}") private val namespace: String,
	@Value("\${NAIS_APP_NAME:\${spring.application.name}}") private val appnavn: String,
	@Value("\${app.dine-muligheter-url}") private val dineMuligheterUrl: String,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	fun sendBeskjed(request: SendVarselRequest): SendResult {
		val ident = request.ident.trim()

		if (!ident.matches(Regex("\\d{11}"))) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ident må bestå av 11 siffer")
		}

		val melding = try {
			VarselActionBuilder.opprett {
				this.type = Varseltype.Beskjed
				this.link = dineMuligheterUrl
				this.varselId = request.varselId
				this.ident = ident
				this.sensitivitet = Sensitivitet.Substantial
				this.tekst = Tekst(
					spraakkode = "nb",
					tekst = UNGE_MED_UFORE_VARSEL,
					default = true,
				)
				this.eksternVarsling {
					preferertKanal = EksternKanal.SMS
					smsVarslingstekst = UNGE_MED_UFORE_SMS
				}
				this.produsent = Produsent(cluster, namespace, appnavn)
				this.aktivFremTil = ZonedDateTime.of(LocalDateTime.of(2026, 12, 31, 23, 59), ZoneId.of("Europe/Oslo"))
			}
		} catch (e: VarselValidationException) {
			logger.error("Validering av varsel feilet for varselId=${request.varselId}: ${e.errors}", e)
			throw e
		}

		kafkaTemplate.send(producerTopic, request.varselId, melding).get(1, TimeUnit.SECONDS)

		logger.info("Sendte varsel med id=${request.varselId}")
		return SendResult(request.varselId)
	}
}

data class SendResult(val varselId: String)

data class SendVarselRequest(
	val varselId: String,
	val ident: String,
)
