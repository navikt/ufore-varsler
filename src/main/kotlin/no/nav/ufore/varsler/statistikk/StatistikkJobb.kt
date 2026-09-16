package no.nav.ufore.varsler.statistikk

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("statistikk-jobb")
class StatistikkJobb(private val service: StatistikkService) : ApplicationRunner {
    override fun run(args: ApplicationArguments) = service.execute()
}
