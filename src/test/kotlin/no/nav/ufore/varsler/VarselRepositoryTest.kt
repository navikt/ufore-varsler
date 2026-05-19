package no.nav.ufore.varsler

import org.flywaydb.core.Flyway
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

class VarselRepositoryTest {

	@Test
	fun `Skal lagre varsel`() {
		val dataSource = dataSource()
		val jdbcTemplate = JdbcTemplate(dataSource)
		val repository = VarselRepository(jdbcTemplate)
		val planlagtUtsending = LocalDateTime.of(2026, 5, 8, 12, 30)

		val varsel = repository.lagre(
			fnr = "12345678910",
			type = VarselType.UNG_UFOR,
			planlagtUtsending = planlagtUtsending,
		)

		assertNotNull(varsel.id)
		assertEquals("12345678910", varsel.mottakerFnr)
		assertEquals(Status.IKKE_SENDT, varsel.status)
		assertEquals(VarselType.UNG_UFOR, varsel.type)
		assertEquals(planlagtUtsending, varsel.planlagtUtsending)
		assertNull(varsel.åpnet)
		assertNull(varsel.sendt)

		val hentetVarsel = assertNotNull(repository.hent(varsel.id.toString()))
		assertEquals(varsel.id, hentetVarsel.id)
		assertEquals("12345678910", hentetVarsel.mottakerFnr)
		assertEquals(Status.IKKE_SENDT, hentetVarsel.status)
		assertEquals(VarselType.UNG_UFOR, hentetVarsel.type)
		println("varsel.opprettet ${varsel.opprettet}")
		println("hentetVarsel.opprettet ${hentetVarsel.opprettet}")
		assertEquals(varsel.opprettet.withNano(0), hentetVarsel.opprettet.withNano(0))
		assertEquals(planlagtUtsending, hentetVarsel.planlagtUtsending)
		assertNull(hentetVarsel.åpnet)
		assertNull(hentetVarsel.sendt)
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
