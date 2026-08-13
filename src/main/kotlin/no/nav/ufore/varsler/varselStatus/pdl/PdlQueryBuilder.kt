package no.nav.ufore.varsler.varselStatus.pdl

class PdlQueryBuilder {
    companion object {
        fun getAdressebeskyttelseQuery(pid: String): PdlPersonQuery {
            return getPdlQuery(pid, "/adressebeskyttelse.graphql")
        }

        private fun getPdlQuery(pid: String, queryFilePath: String): PdlPersonQuery {
            return PdlPersonQuery(
                query = PdlPersonQuery::class.java.getResource(queryFilePath)
                    ?.readText()?.replace("[ \n\r]", "")
                    ?: throw IllegalArgumentException("Unable to locate graphQl file"),
                variables = PdlPersonVariables(pid)
            )
        }
    }
}

data class PdlPersonQuery(
    val query: String,
    val variables: PdlPersonVariables,
)

data class PdlPersonVariables(
    val ident: String,
)