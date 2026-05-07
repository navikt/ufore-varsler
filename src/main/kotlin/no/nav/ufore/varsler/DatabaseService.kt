package no.nav.ufore.varsler

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class DatabaseService(
	private val jdbcTemplate: JdbcTemplate,
) {
	fun isDatabaseUp(): Boolean =
		runCatching {
			jdbcTemplate.queryForObject("select 1", Int::class.java) == 1
		}.getOrDefault(false)
}
