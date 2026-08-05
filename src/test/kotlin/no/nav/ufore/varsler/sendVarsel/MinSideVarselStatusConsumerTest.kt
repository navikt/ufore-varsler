package no.nav.ufore.varsler.sendVarsel

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tms.varsel.action.Varseltype
import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.Varsel
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import no.nav.ufore.varsler.opprettVarsel.VarselType
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MinSideVarselStatusConsumerTest {

    val consumerAppName = "ufore-varsler"
    val objectMapper = ObjectMapper()
    val varselRepository = mockk<VarselRepository>(relaxed = true)
    val meterRegistry = SimpleMeterRegistry()
    val consumer = MinSideVarselStatusConsumer(objectMapper, varselRepository, meterRegistry, consumerAppName)

    val varsel = Varsel(
        id = UUID.randomUUID(),
        varselId = UUID.randomUUID(),
        mottakerFnr = "1234",
        status = Status.OPPRETTET,
        type = VarselType.UNGE_MED_UFORE,
        opprettet = LocalDateTime.now(),
        bestilt = null,
        sendt = null,
        åpnet = null,
    )

    val hendelse = MinSideVarselHendelse(
        eventName = MinSideEventName.eksternStatusOppdatert,
        status = MinSideEksternStatus.sendt,
        varselType = Varseltype.Beskjed,
        varselId = UUID.randomUUID().toString(),
        namespace = "ufore",
        appnavn = "ufore-varsler-send-jobb",
        tidspunkt = ZonedDateTime.now(),
    )

    @Test
    fun `Skal kaste exception og telle feil_rekkefolge hvis sendt-hendelse ankommer med feil DB-status`() {
        every { varselRepository.hent(any()) } returns varsel // status=OPPRETTET, ikke BESTILT

        val melding = ObjectMapper().writeValueAsString(hendelse)
        assertThrows<FeilStatusException> {
            consumer.consume(melding)
        }

        verify(exactly = 0) { varselRepository.oppdaterSendt(any()) }
        assertEquals(1.0, meterRegistry.counter("ufore_varsler_status_feil_total", "aarsak", "feil_rekkefolge").count())
    }

    @Test
    fun `Skal gå videre hvis varselId ikke er en gyldig UUID`() {
        val hendelseMedUgyldigVarselId = hendelse.copy(varselId = "ugyldig id")
        val melding = ObjectMapper().writeValueAsString(hendelseMedUgyldigVarselId)

        consumer.consume(melding)

        verify(exactly = 0) { varselRepository.oppdaterSendt(any()) }
        verify(exactly = 0) { varselRepository.hent(any()) }
    }

    @Test
    fun `Skal oppdatere status til sendt når hendelse ankommer med riktig DB-status`() {
        every { varselRepository.hent(any()) } returns varsel.copy(status = Status.BESTILT)

        val melding = ObjectMapper().writeValueAsString(hendelse)
        consumer.consume(melding)

        verify(exactly = 1) { varselRepository.oppdaterSendt(any()) }
        assertEquals(1.0, meterRegistry.counter("ufore_varsler_status_total", "status", "sendt").count())
    }

    @Test
    fun `Skal oppdatere til åpnet når inaktivert ankommer etter sendt`() {
        every { varselRepository.hent(any()) } returns varsel.copy(status = Status.SENDT)
        val inaktivertHendelse = hendelse.copy(eventName = MinSideEventName.inaktivert, status = null)
        val melding = ObjectMapper().writeValueAsString(inaktivertHendelse)

        consumer.consume(melding)

        verify(exactly = 1) { varselRepository.oppdaterÅpnet(any()) }
        assertEquals(1.0, meterRegistry.counter("ufore_varsler_status_total", "status", "aapnet").count())
    }
}
