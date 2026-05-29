package no.nav.ufore.varsler.opprettVarsel

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.time.LocalDateTime
import java.util.UUID
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
		val planlagtUtsending = LocalDateTime.of(2026, 5, 8, 12, 30)

		val varsel = repository.lagre(
			fnr = "12345678910",
			type = VarselType.UNGE_MED_UFORE,
			planlagtUtsending = planlagtUtsending,
		)

        assertNotNull(varsel.id)
        assertEquals("12345678910", varsel.mottakerFnr)
        assertEquals(Status.IKKE_SENDT, varsel.status)
        assertEquals(VarselType.UNGE_MED_UFORE, varsel.type)
        assertEquals(planlagtUtsending, varsel.planlagtUtsending)
        assertNull(varsel.åpnet)
        assertNull(varsel.sendt)

		val hentetVarsel = assertNotNull(repository.hent(varsel.id.toString()))
        assertEquals(varsel.id, hentetVarsel.id)
        assertEquals("12345678910", hentetVarsel.mottakerFnr)
        assertEquals(Status.IKKE_SENDT, hentetVarsel.status)
        assertEquals(VarselType.UNGE_MED_UFORE, hentetVarsel.type)
		println("varsel.opprettet ${varsel.opprettet}")
		println("hentetVarsel.opprettet ${hentetVarsel.opprettet}")
        assertEquals(varsel.opprettet.withNano(0), hentetVarsel.opprettet.withNano(0))
        assertEquals(planlagtUtsending, hentetVarsel.planlagtUtsending)
        assertNull(hentetVarsel.åpnet)
        assertNull(hentetVarsel.sendt)
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