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
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MinSideVarselStatusConsumerTest {

    val appName = "ufore-varsler-send-jobb"
    val objectMapper = ObjectMapper()
    val varselRepository = mockk<VarselRepository>(relaxed = true)
    val meterRegistry = SimpleMeterRegistry()
    val consumer = MinSideVarselStatusConsumer(objectMapper, varselRepository, meterRegistry, appName)

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
        appnavn = appName,
        tidspunkt = ZonedDateTime.now(),
    )

    @Test
    fun `Skal hoppe over og telle feil_rekkefolge hvis sendt-hendelse ankommer med feil DB-status`() {
        every { varselRepository.hent(any()) } returns varsel // status=OPPRETTET, ikke BESTILT

        val melding = ObjectMapper().writeValueAsString(hendelse)
        consumer.consume(melding)

        verify(exactly = 0) { varselRepository.oppdaterSendt(any()) }
        assertEquals(1.0, meterRegistry.counter("ufore_varsler_status_feil_total", "aarsak", "feil_rekkefolge").count())
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
    fun `Skal oppdatere til åpnet selv om inaktivert ankommer før sendt-hendelse er prosessert`() {
        // Kafka garanterer ikke rekkefølge — inaktivert kan komme før sendt
        every { varselRepository.hent(any()) } returns varsel.copy(status = Status.BESTILT)
        val inaktivertHendelse = hendelse.copy(eventName = MinSideEventName.inaktivert, status = null)
        val melding = ObjectMapper().writeValueAsString(inaktivertHendelse)

        consumer.consume(melding)

        verify(exactly = 1) { varselRepository.oppdaterÅpnet(any()) }
        assertEquals(1.0, meterRegistry.counter("ufore_varsler_status_total", "status", "aapnet").count())
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

    @Test
    fun `Skal hoppe over inaktivert-hendelse hvis varsel ikke finnes i DB`() {
        every { varselRepository.hent(any()) } returns null
        val inaktivertHendelse = hendelse.copy(eventName = MinSideEventName.inaktivert, status = null)
        val melding = ObjectMapper().writeValueAsString(inaktivertHendelse)

        consumer.consume(melding)

        verify(exactly = 0) { varselRepository.oppdaterÅpnet(any()) }
    }
}
