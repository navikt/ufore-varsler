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
        assertEquals(Status.IKKE_SENDT, varsel.status)
        assertEquals(VarselType.UNGE_MED_UFORE, varsel.type)
        assertNull(varsel.åpnet)
        assertNull(varsel.sendt)
		assertNotNull(varsel.varselId)

		val hentetVarsel = assertNotNull(repository.hent(varsel.id.toString()))
        assertEquals(varsel.id, hentetVarsel.id)
        assertEquals("12345678910", hentetVarsel.mottakerFnr)
        assertEquals(Status.IKKE_SENDT, hentetVarsel.status)
        assertEquals(VarselType.UNGE_MED_UFORE, hentetVarsel.type)
        assertEquals(varsel.opprettet.withNano(0), hentetVarsel.opprettet.withNano(0))
        assertNull(hentetVarsel.åpnet)
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

	private fun dataSource() = DriverManagerDataSource(
		"jdbc:h2:mem:ufore_varsler;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"sa",
		"",
	).apply {
		setDriverClassName("org.h2.Driver")
	}.also {
		Flyway.configure().dataSource(it).load().migrate()
	}
}