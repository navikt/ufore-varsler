package no.nav.ufore.varsler.opprettVarsel

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VarselRepositoryTest {

    val dataSource = dataSource()
    val jdbcTemplate = JdbcTemplate(dataSource)

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("truncate table varsel")
    }

	@Test
	fun `Skal lagre varsel`() {
		val repository = VarselRepository(jdbcTemplate)

		val varsel = repository.lagre(
			fnr = "12345678910",
			type = VarselType.UNGE_MED_UFORE
		)

        assertNotNull(varsel.id)
        assertEquals("12345678910", varsel.mottakerFnr)
        assertEquals(Status.OPPRETTET, varsel.status)
        assertEquals(VarselType.UNGE_MED_UFORE, varsel.type)
        assertNull(varsel.åpnet)
        assertNull(varsel.bestilt)
        assertNull(varsel.sendt)
		assertNotNull(varsel.varselId)

		val hentetVarsel = assertNotNull(repository.hent(varsel.varselId.toString()))
        assertEquals(varsel.id, hentetVarsel.id)
        assertEquals("12345678910", hentetVarsel.mottakerFnr)
        assertEquals(Status.OPPRETTET, hentetVarsel.status)
        assertEquals(VarselType.UNGE_MED_UFORE, hentetVarsel.type)
        assertEquals(varsel.opprettet.withNano(0), hentetVarsel.opprettet.withNano(0))
        assertNull(hentetVarsel.åpnet)
        assertNull(hentetVarsel.bestilt)
        assertNull(hentetVarsel.sendt)
		assertEquals(varsel.varselId, hentetVarsel.varselId)
	}

	@Test
	fun `Skal ikke kunne lagre to varsler med samme fnr og type`() {
		val dataSource = dataSource()
		val jdbcTemplate = JdbcTemplate(dataSource)
		val repository = VarselRepository(jdbcTemplate)

		repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)

		assertFailsWith<DataIntegrityViolationException> {
			repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)
		}
	}

	@Test
	fun `Skal hente varsel på fnr og type`() {
		val repository = VarselRepository(jdbcTemplate)

		val lagret = repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)

		val hentet = assertNotNull(repository.hent("12345678910", VarselType.UNGE_MED_UFORE))
		assertEquals(lagret.id, hentet.id)
	}

	@Test
	fun `Skal returnere null når varsel ikke finnes for fnr og type`() {
		val repository = VarselRepository(jdbcTemplate)

		val hentet = repository.hent("12345678910", VarselType.UNGE_MED_UFORE)
		assertNull(hentet)
	}

	@Test
	fun `Skal hente varsler med status OPPRETTET`() {
		val repository = VarselRepository(jdbcTemplate)

		val varsel = repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)
        val sendtVarsel = repository.lagre(fnr = "123459156", type = VarselType.UNGE_MED_UFORE)

        repository.oppdaterSendt(sendtVarsel.varselId.toString())

		val opprettet = repository.hentOpprettet(10)

		assertEquals(1, opprettet.size)
		assertEquals(varsel.id, opprettet[0].id)
	}

	@Test
	fun `Skal begrense antall returnerte varsler`() {
		val repository = VarselRepository(jdbcTemplate)

		repository.lagre(fnr = "12345678901", type = VarselType.UNGE_MED_UFORE)
		repository.lagre(fnr = "12345678902", type = VarselType.UNGE_MED_UFORE)
		repository.lagre(fnr = "12345678903", type = VarselType.UNGE_MED_UFORE)

		val opprettet = repository.hentOpprettet(2)

		assertEquals(2, opprettet.size)
	}

	@Test
	fun `Skal telle antall bestilt i dag`() {
		val repository = VarselRepository(jdbcTemplate)

        assertEquals(0, repository.antallBestiltIDag())

		val varsel1 = repository.lagre(fnr = "12345678901", type = VarselType.UNGE_MED_UFORE)
		val varsel2 = repository.lagre(fnr = "12345678902", type = VarselType.UNGE_MED_UFORE)
        val ikkeBestilt = repository.lagre(fnr = "12345678903", type = VarselType.UNGE_MED_UFORE)
		repository.oppdaterBestilt(varsel1.varselId.toString())
		repository.oppdaterBestilt(varsel2.varselId.toString())

		assertEquals(2, repository.antallBestiltIDag())
	}

	@Test
	fun `Skal oppdatere varsel til BESTILT`() {
		val repository = VarselRepository(jdbcTemplate)

		val varsel = repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)
		repository.oppdaterBestilt(varsel.varselId.toString())

		val oppdatert = assertNotNull(repository.hent(varsel.varselId.toString()))
		assertEquals(Status.BESTILT, oppdatert.status)
		assertNotNull(oppdatert.bestilt)
	}

	@Test
	fun `Skal oppdatere varsel til ÅPNET`() {
		val repository = VarselRepository(jdbcTemplate)

		val varsel = repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)
		repository.oppdaterÅpnet(varsel.varselId.toString())

		val oppdatert = assertNotNull(repository.hent(varsel.varselId.toString()))
		assertEquals(Status.ÅPNET, oppdatert.status)
		assertNotNull(oppdatert.åpnet)
	}

	@Test
	fun `Skal oppdatere varsel til SENDT`() {
		val repository = VarselRepository(jdbcTemplate)

		val varsel = repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)
		repository.oppdaterSendt(varsel.varselId.toString())

		val oppdatert = assertNotNull(repository.hent(varsel.varselId.toString()))
		assertEquals(Status.SENDT, oppdatert.status)
		assertNotNull(oppdatert.sendt)
	}

	@Test
	fun `Skal oppdatere varsel til FEILET`() {
		val repository = VarselRepository(jdbcTemplate)

		val varsel = repository.lagre(fnr = "12345678910", type = VarselType.UNGE_MED_UFORE)
		repository.oppdaterFeilet(varsel.varselId.toString())

		val oppdatert = assertNotNull(repository.hent(varsel.varselId.toString()))
		assertEquals(Status.FEILET, oppdatert.status)
	}

	private fun dataSource() = DriverManagerDataSource(
		"jdbc:h2:mem:ufore_varsler;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"sa",
		"",
	).apply {
		setDriverClassName("org.h2.Driver")
	}.also {
		Flyway.configure().dataSource(it).locations("classpath:db/migration/common").load().migrate()
	}
}