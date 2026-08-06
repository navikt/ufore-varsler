package no.nav.ufore.varsler.varselStatus

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/varsler")
class VarselStatusController(
    private val varselStatusService: VarselStatusService,
    private val tokenValidator: TexasTokenValidator,
) {

    @PostMapping("/status")
    fun hentStatus(@RequestHeader("Authorization", required = false) authHeader: String?, @RequestBody requestBody: VarselStatusRequest?): ResponseEntity<VarselStatusResponse> {
        val token = authHeader?.removePrefix("Bearer ") ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val brukerType = tokenValidator.hentBrukertype(token)

        val gyldigToken = tokenValidator.sjekkGyldigToken(token, brukerType)

        val fnr = when (brukerType) {
            TexasTokenValidator.Bruker.Borger -> gyldigToken.pid
            TexasTokenValidator.Bruker.Veileder -> requestBody?.pid
        } ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(VarselStatusResponse(varselStatusService.harMottattVarsel(fnr)))
    }
}

data class VarselStatusResponse(val harMottattVarsel: Boolean)

data class VarselStatusRequest(val pid: String)
