package no.nav.ufore.varsler.sendVarsel

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.FakeUnleash
import io.mockk.every
import io.mockk.verify
import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import no.nav.ufore.varsler.opprettVarsel.VarselType
import no.nav.ufore.varsler.sendVarsel.SendVarslerService.Companion.maksAntallPerDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local")
@SpringBootTest
class SendVarslerServiceTest {

    @MockkBean
    lateinit var sendVarselProducer: SendVarselProducer

    @Autowired
    lateinit var sendVarslerService: SendVarslerService

    @Autowired
    lateinit var varselRepository: VarselRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var unleash: FakeUnleash

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("truncate table varsel")
        unleash.enableAll()
    }

    @Test
    fun `Skal sende varsler og oppdatere status til SENDT`() {
        varselRepository.lagre("12345678901", VarselType.UNGE_MED_UFORE)

        every { sendVarselProducer.sendBeskjed(any()) } returns SendResult("123")

        sendVarslerService.execute()

        val varsel = varselRepository.hent("12345678901", VarselType.UNGE_MED_UFORE)
        assertEquals(Status.BESTILT, varsel?.status)

        verify(exactly = 1) { sendVarselProducer.sendBeskjed(any()) }
    }

    @Test
    fun `Skal håndtere feil ved sending av varsel`() {
        varselRepository.lagre("12345678901", VarselType.UNGE_MED_UFORE)

        every { sendVarselProducer.sendBeskjed(any()) } throws Exception("Noe gikk galt")

        sendVarslerService.execute()

        val varsel = varselRepository.hent("12345678901", VarselType.UNGE_MED_UFORE)
        assertEquals(Status.OPPRETTET, varsel?.status)
    }

    @Test
    fun `Skal ikke sende varsler når jobb er slått av`() {
        unleash.disableAll()

        varselRepository.lagre("12345678901", VarselType.UNGE_MED_UFORE)

        sendVarslerService.execute()

        val varsel = varselRepository.hent("12345678901", VarselType.UNGE_MED_UFORE)
        assertEquals(Status.OPPRETTET, varsel?.status)
    }

    @Test
    fun `Skal ikke sende varsler hvis maks antall er sendt`() {
        for (i in 1..maksAntallPerDag + 10) {
            varselRepository.lagre(i.toString(), VarselType.UNGE_MED_UFORE)
        }

        every { sendVarselProducer.sendBeskjed(any()) } returns SendResult("123")

        sendVarslerService.execute() // nå er maks antall per dag sendt
        sendVarslerService.execute() // skal ikke sende flere i dag

        val ikkeSendt = varselRepository.hentOpprettet(100)
        assertEquals(10, ikkeSendt.size)

        val sendtIDag = varselRepository.antallBestiltIDag()
        assertEquals(maksAntallPerDag, sendtIDag)

        verify(exactly = maksAntallPerDag) { sendVarselProducer.sendBeskjed(any()) }
    }
}
