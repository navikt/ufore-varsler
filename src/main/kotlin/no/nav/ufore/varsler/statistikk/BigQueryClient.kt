package no.nav.ufore.varsler.statistikk

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("statistikk-jobb")
class BigQueryClient(
    @Value("\${app.statistikk.bigquery.dataset}") private val dataset: String,
) {
    private val bigQuery: BigQuery = BigQueryOptions.getDefaultInstance().service
    private val table = "`$dataset.varsel_statistikk`"

    fun erstattStatistikkFor(statistikk: List<VarselStatusStatistikk>) {
        opprettTabellHvisDenMangler()

        statistikk.forEach { rad ->
            bigQuery.query(
                QueryJobConfiguration.newBuilder(
                    """
                    merge $table as eksisterende
                    using (
                        select
                            @rapportdato as rapportdato,
                            @type as type,
                            @status as status,
                            @antall as antall
                    ) as ny
                    on eksisterende.rapportdato = ny.rapportdato
                       and eksisterende.type = ny.type
                       and eksisterende.status = ny.status
                    when matched then
                        update set antall = ny.antall
                    when not matched then
                        insert (rapportdato, type, status, antall)
                        values (ny.rapportdato, ny.type, ny.status, ny.antall)
                    """.trimIndent(),
                )
                    .setUseLegacySql(false)
                    .addNamedParameter(
                        "rapportdato",
                        QueryParameterValue.date(rad.rapportdato.toString()),
                    )
                    .addNamedParameter("type", QueryParameterValue.string(rad.type))
                    .addNamedParameter("status", QueryParameterValue.string(rad.status))
                    .addNamedParameter("antall", QueryParameterValue.int64(rad.antall))
                    .build(),
            )
        }
    }

    private fun opprettTabellHvisDenMangler() {
        bigQuery.query(
            QueryJobConfiguration.newBuilder(
                """
                create table if not exists $table (
                    rapportdato date not null,
                    type string not null,
                    status string not null,
                    antall int64 not null
                )
                partition by rapportdato
                """.trimIndent(),
            )
                .setUseLegacySql(false)
                .build(),
        )
    }
}
