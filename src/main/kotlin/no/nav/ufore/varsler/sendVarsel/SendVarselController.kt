package no.nav.ufore.varsler.sendVarsel

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/varsler")
class SendVarselController(
    private val varselProducer: SendVarselProducer,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@PostMapping("/beskjed")
	fun sendBeskjed(@RequestBody request: SendBeskjedRequest): ResponseEntity<SendBeskjedResponse> {
		logger.info("Sender varsel...")

		val resultat = varselProducer.sendBeskjed(request)

		return ResponseEntity
			.status(HttpStatus.ACCEPTED)
			.body(SendBeskjedResponse(varselId = resultat.varselId))
	}
}

data class SendBeskjedRequest(
	val ident: String,
	val tekst: String,
)

data class SendBeskjedResponse(val varselId: String)
