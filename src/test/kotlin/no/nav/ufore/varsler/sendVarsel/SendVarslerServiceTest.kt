package no.nav.ufore.varsler.sendVarsel

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.ufore.varsler.opprettVarsel.Status
import no.nav.ufore.varsler.opprettVarsel.VarselRepository
import no.nav.ufore.varsler.opprettVarsel.VarselType
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

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("truncate table varsel")
    }

    @Test
    fun `Skal sende varsler og oppdatere status til SENDT`() {
        varselRepository.lagre("12345678901", VarselType.UNGE_MED_UFORE)

        every { sendVarselProducer.sendBeskjed(any()) } returns SendResult("123")

        sendVarslerService.execute()

        val varsel = varselRepository.hent("12345678901", VarselType.UNGE_MED_UFORE)
        assertEquals(Status.SENDT, varsel?.status)
    }

    @Test
    fun `Skal håndtere feil ved sending av varsel`() {
        varselRepository.lagre("12345678901", VarselType.UNGE_MED_UFORE)

        every { sendVarselProducer.sendBeskjed(any()) } throws Exception("Noe gikk galt")

        sendVarslerService.execute()

        val varsel = varselRepository.hent("12345678901", VarselType.UNGE_MED_UFORE)
        assertEquals(Status.IKKE_SENDT, varsel?.status)
    }

    @Test
    fun `Skal ikke sende varsler når jobb er slått av`() {
        // TODO: test med FakeUnleash som har flagget deaktivert
    }
}
