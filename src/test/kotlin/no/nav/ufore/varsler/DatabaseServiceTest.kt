package no.nav.ufore.varsler

import kotlin.test.Test
import kotlin.test.assertTrue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

class DatabaseServiceTest {

	@Test
	fun `Skal få kontakt med databasen`() {
		val dataSource = DriverManagerDataSource(
			"jdbc:h2:mem:database_status;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
			"sa",
			"",
		)
		dataSource.setDriverClassName("org.h2.Driver")

		val service = DatabaseService(JdbcTemplate(dataSource))

		assertTrue(service.isDatabaseUp())
	}
}
