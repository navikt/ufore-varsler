package no.nav.ufore.varsler.sendVarsel

import io.mockk.every
import io.mockk.mockk
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

class MinSideVarselStatusConsumerTest {

    val objectMapper = ObjectMapper()
    val varselRepository = mockk<VarselRepository>()
    val consumer = MinSideVarselStatusConsumer(objectMapper, varselRepository)

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
        status =  MinSideEksternStatus.sendt,
        varselType = Varseltype.Beskjed,
        varselId = UUID.randomUUID().toString(),
        namespace = "ufore",
        appnavn = "ufore-varsler",
        tidspunkt = ZonedDateTime.now(),
    )

    @Test
    fun `Skal kaste exception hvis vi prøver sette status sendt før bestilt`() {
        every { varselRepository.hent(any()) } returns varsel

        val melding = ObjectMapper().writeValueAsString(hendelse)

        assertThrows<FeilStatusException> {
            consumer.consume(melding)
        }
    }

    @Test
    fun `Skal kaste exception hvis vi prøver sette status åpnet før sendt`() {
        every { varselRepository.hent(any()) } returns varsel.copy(status = Status.OPPRETTET)
        val hendelse = hendelse.copy(eventName = MinSideEventName.inaktivert)
        val melding = ObjectMapper().writeValueAsString(hendelse)

        assertThrows<FeilStatusException> {
            consumer.consume(melding)
        }
    }
}