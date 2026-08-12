package no.nav.ufore.varsler.varselStatus

import no.nav.ufore.varsler.varselStatus.pdl.PdlClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    fun hentStatus(@RequestHeader("Authorization", required = false) authHeader: String?, @RequestBody requestBody: VarselStatusRequest?): ResponseEntity<VarselStatusResponse> {
        val token = authHeader?.removePrefix("Bearer ") ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        return ResponseEntity.ok(VarselStatusResponse(varselStatusService.hentStatus(token, requestBody?.pid)))
    }
}

data class VarselStatusResponse(val harMottattVarsel: Boolean)

data class VarselStatusRequest(val pid: String)
