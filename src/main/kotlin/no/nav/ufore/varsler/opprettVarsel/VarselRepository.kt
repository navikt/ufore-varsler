package no.nav.ufore.varsler.opprettVarsel

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Service
class VarselRepository(private val jdbcTemplate: JdbcTemplate) {

	fun lagre(fnr: String, type: VarselType): Varsel {
		val varsel = Varsel(
			id = UUID.randomUUID(),
			mottakerFnr = fnr,
			status = Status.IKKE_SENDT,
			type = type,
			varselId = UUID.randomUUID(),
			opprettet = LocalDateTime.now(),
			åpnet = null,
			sendt = null,
		)

		jdbcTemplate.update(
			"""
				insert into varsel (id, mottaker_fnr, status, type, opprettet, aapnet, sendt, varsel_id)
				values (?, ?, ?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			varsel.id,
			varsel.mottakerFnr,
			varsel.status.name,
			varsel.type.name,
			varsel.opprettet,
			varsel.åpnet,
			varsel.sendt,
			varsel.varselId,
		)

		return varsel
	}

	fun hent(id: String): Varsel? {
		return jdbcTemplate.query(
			"select * from varsel where id = ?",
			{ rs, _ -> Varsel.tilVarsel(rs) },
			id
		).firstOrNull()
	}

    fun hent(fnr: String, type: VarselType): Varsel? {
        return jdbcTemplate.query(
            "select * from varsel where mottaker_fnr = ? and type = ?",
            { rs, _ -> Varsel.tilVarsel(rs) },
            fnr, type.name
        ).firstOrNull()
    }

    fun hentIkkeSendte(antall: Int): List<Varsel> {
        return jdbcTemplate.query(
            "select * from varsel where status = ? LIMIT ?",
            { rs, _ -> Varsel.tilVarsel(rs) },
            Status.IKKE_SENDT.name, antall
        )
    }

    fun antallSendtIDag(): Int {
        return jdbcTemplate.queryForObject(
            "select count(*) from varsel where DATE(sendt) = CURRENT_DATE and status != ?",
            { rs, _ -> rs.getInt(1) },
            Status.SENDT.name
        )
    }

    fun oppdaterStatus(id: String, status: Status) {
        jdbcTemplate.update(
            "update varsel set status = ? where id = ?",
            status.name, id
        )
    }
}


data class Varsel(
	val id: UUID,
	val mottakerFnr: String,
	val status: Status,
	val type: VarselType,
	val varselId: UUID,
	val opprettet: LocalDateTime,
	val åpnet: LocalDateTime?,
	val sendt: LocalDateTime?,
) {
    companion object {
        fun tilVarsel(rs: ResultSet) = Varsel(
            id = rs.getObject("id", UUID::class.java),
            mottakerFnr = rs.getString("mottaker_fnr"),
            status = Status.valueOf(rs.getString("status")),
            type = VarselType.valueOf(rs.getString("type")),
			varselId = rs.getObject("varsel_id", UUID::class.java),
            opprettet = rs.getObject("opprettet", LocalDateTime::class.java),
            åpnet = rs.getObject("aapnet", LocalDateTime::class.java),
            sendt = rs.getObject("sendt", LocalDateTime::class.java),
        )
    }
}

enum class Status {
	IKKE_SENDT, SENDT, ÅPNET, FEIL
}

enum class VarselType {
    UNGE_MED_UFORE
}