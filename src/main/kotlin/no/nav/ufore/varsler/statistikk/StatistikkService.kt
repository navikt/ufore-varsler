package no.nav.ufore.varsler.statistikk

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@Profile("statistikk-jobb")
class StatistikkService(
    private val jdbcTemplate: JdbcTemplate,
    private val bigQueryClient: BigQueryClient,

    ) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute() {
        logger.info("Aggregerer statistikk for ufore-varsler")

        val statistikk = jdbcTemplate.query(
            """
            select type, status, count(*) as antall
            from varsel
            group by type, status
            """.trimIndent(),
        ) { rs, _ ->
            VarselStatusStatistikk(
                rapportdato = LocalDate.now(),
                type = rs.getString("type"),
                status = rs.getString("status"),
                antall = rs.getLong("antall"),
            )
        }

        bigQueryClient.erstattStatistikkFor(statistikk)

    }
}

data class VarselStatusStatistikk(
    val rapportdato: LocalDate,
    val type: String,
    val status: String,
    val antall: Long,
)
