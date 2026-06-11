package no.nav.ufore.varsler.varselStatus

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/varsler")
class VarselStatusController(
    private val varselStatusService: VarselStatusService,
    private val tokenValidator: TexasTokenValidator,
) {

    @GetMapping("/status")
    fun hentStatus(@RequestHeader("Authorization", required = false) authHeader: String?): ResponseEntity<VarselStatusResponse> {
        val fnr = authHeader?.let(tokenValidator::hentFnr)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(VarselStatusResponse(varselStatusService.harMottattVarsel(fnr)))
    }
}

data class VarselStatusResponse(val harMottattVarsel: Boolean)
