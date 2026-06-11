package no.nav.ufore.varsler.sendVarsel

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("job")
class SendVarslerJobb(private val service: SendVarslerService) : ApplicationRunner {
    override fun run(args: ApplicationArguments) = service.execute()
}
