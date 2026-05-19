package no.nav.ufore.varsler

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class VarselRepository(private val jdbcTemplate: JdbcTemplate) {

	fun lagre(fnr: String, type: VarselType, planlagtUtsending: LocalDateTime? = null): Varsel {
		val varsel = Varsel(
			id = UUID.randomUUID(),
			mottakerFnr = fnr,
			status = Status.IKKE_SENDT,
			type = type,
			opprettet = LocalDateTime.now(),
			planlagtUtsending = planlagtUtsending,
			åpnet = null,
			sendt = null,
		)

		jdbcTemplate.update(
			"""
				insert into varsel (id, mottaker_fnr, status, type, opprettet, planlagt_utsending, aapnet, sendt)
				values (?, ?, ?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			varsel.id,
			varsel.mottakerFnr,
			varsel.status.name,
			varsel.type.name,
			varsel.opprettet,
			varsel.planlagtUtsending,
			varsel.åpnet,
			varsel.sendt,
		)

		return varsel
	}

	fun hent(id: String): Varsel? {
		return jdbcTemplate.query(
			"select * from varsel where id = ?",
			{ rs, _ ->
				Varsel(
					id = rs.getObject("id", UUID::class.java),
					mottakerFnr = rs.getString("mottaker_fnr"),
					status = Status.valueOf(rs.getString("status")),
					type = VarselType.valueOf(rs.getString("type")),
					opprettet = rs.getObject("opprettet", LocalDateTime::class.java),
					planlagtUtsending = rs.getObject("planlagt_utsending", LocalDateTime::class.java),
					åpnet = rs.getObject("aapnet", LocalDateTime::class.java),
					sendt = rs.getObject("sendt", LocalDateTime::class.java),
				)
			},
			id
		).firstOrNull()
	}
}

data class Varsel(
	val id: UUID,
	val mottakerFnr: String,
	val status: Status,
	val type: VarselType,

	val opprettet: LocalDateTime,
	val planlagtUtsending: LocalDateTime?,
	val åpnet: LocalDateTime?,
	val sendt: LocalDateTime?,
)

enum class Status {
	IKKE_SENDT, SENDT, ÅPNET, FEIL
}

enum class VarselType {
	UNG_UFOR
}
