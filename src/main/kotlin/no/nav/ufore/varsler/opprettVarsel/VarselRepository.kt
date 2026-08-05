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
			status = Status.OPPRETTET,
			type = type,
			varselId = UUID.randomUUID(),
			opprettet = LocalDateTime.now(),
            bestilt = null,
            sendt = null,
			åpnet = null,
            erÅpnet = false
		)

		jdbcTemplate.update(
			"""
				insert into varsel (id, mottaker_fnr, status, type, opprettet, aapnet, bestilt, sendt, varsel_id)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			varsel.id,
			varsel.mottakerFnr,
			varsel.status.name,
			varsel.type.name,
			varsel.opprettet,
			varsel.åpnet,
            varsel.bestilt,
			varsel.sendt,
			varsel.varselId,
		)

		return varsel
	}

	fun hent(varselId: UUID): Varsel? {
		return jdbcTemplate.query(
			"select * from varsel where varsel_id = ?",
			{ rs, _ -> Varsel.tilVarsel(rs) },
			varselId
		).firstOrNull()
	}

    fun hent(fnr: String, type: VarselType): Varsel? {
        return jdbcTemplate.query(
            "select * from varsel where mottaker_fnr = ? and type = ?",
            { rs, _ -> Varsel.tilVarsel(rs) },
            fnr, type.name
        ).firstOrNull()
    }

    fun hentOpprettet(antall: Int): List<Varsel> {
        return jdbcTemplate.query(
            "select * from varsel where status = ? order by opprettet asc LIMIT ?",
            { rs, _ -> Varsel.tilVarsel(rs) },
            Status.OPPRETTET.name, antall
        )
    }

    fun antallBestiltIDag(): Int {
        return jdbcTemplate.queryForObject(
            "select count(*) from varsel where CAST(bestilt AS DATE) = CURRENT_DATE",
            { rs, _ -> rs.getInt(1) },
        )
    }

    fun oppdaterBestilt(varselId: UUID) {
        jdbcTemplate.update(
            "update varsel set status = ?, bestilt = ? where varsel_id = ?",
			Status.BESTILT.name, LocalDateTime.now(), varselId
        )
    }

    fun oppdaterÅpnet(varselId: UUID) {
        jdbcTemplate.update(
            "update varsel set er_aapnet = true, aapnet = ? where varsel_id = ?",
            LocalDateTime.now(), varselId
        )
    }

    fun oppdaterSendt(varselId: UUID) {
        jdbcTemplate.update(
            "update varsel set status = ?, sendt = ? where varsel_id = ?",
            Status.SENDT.name, LocalDateTime.now(), varselId
        )
    }
    fun oppdaterFeilet(varselId: UUID) {
        jdbcTemplate.update(
            "update varsel set status = ? where varsel_id = ?",
            Status.FEILET.name, varselId
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
    val bestilt: LocalDateTime?,
    val sendt: LocalDateTime?,
    val åpnet: LocalDateTime?,
    val erÅpnet: Boolean,
) {
    companion object {
        fun tilVarsel(rs: ResultSet) = Varsel(
            id = rs.getObject("id", UUID::class.java),
            mottakerFnr = rs.getString("mottaker_fnr"),
            status = Status.valueOf(rs.getString("status")),
            type = VarselType.valueOf(rs.getString("type")),
			varselId = rs.getObject("varsel_id", UUID::class.java),
            opprettet = rs.getObject("opprettet", LocalDateTime::class.java),
            bestilt = rs.getObject("bestilt", LocalDateTime::class.java),
            sendt = rs.getObject("sendt", LocalDateTime::class.java),
            åpnet = rs.getObject("aapnet", LocalDateTime::class.java),
            erÅpnet = rs.getBoolean("er_aapnet")
        )
    }
}

enum class Status {
	OPPRETTET, BESTILT, SENDT, FEILET
}

enum class VarselType {
    UNGE_MED_UFORE
}