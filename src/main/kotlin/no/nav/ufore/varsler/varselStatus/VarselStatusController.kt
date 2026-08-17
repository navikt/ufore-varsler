package no.nav.ufore.varsler.varselStatus

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.util.ErrorHandler
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/varsler")
class VarselStatusController(
    private val varselStatusService: VarselStatusService,
) {

    private val logger = LoggerFactory.getLogger(VarselStatusController::class.java)

    @PostMapping("/status")
    fun hentStatus(
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @RequestBody requestBody: VarselStatusRequest?,
        @CookieValue("nav-obo") kryptertRepresentertFnr: String?
    ): ResponseEntity<VarselStatusResponse> {
        val token = authHeader?.removePrefix("Bearer ") ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(VarselStatusResponse(varselStatusService.hentStatus(token, requestBody?.fnr, kryptertRepresentertFnr)))
    }

    @ExceptionHandler(IkkeTilgangException::class)
    fun handle(e: Exception) : ResponseEntity<String> {
        logger.warn(e.message, e)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

}

data class VarselStatusResponse(val harMottattVarsel: Boolean)

data class VarselStatusRequest(val fnr: String)

class IkkeTilgangException(
    val service: String,
    val melding: String,
) : RuntimeException("Ikke tilgang til $service. Melding: $melding")

