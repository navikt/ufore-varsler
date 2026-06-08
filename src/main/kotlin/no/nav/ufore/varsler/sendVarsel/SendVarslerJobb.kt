package no.nav.ufore.varsler.sendVarsel

import io.getunleash.Unleash
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("job")
class SendVarslerJobb(
    private val unleash: Unleash,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        logger.info("Starter jobb")

        if (!unleash.isEnabled("ufore-varsler.send-varsler-jobb")) {
            logger.info("Scheduler er slått av, avslutter")
            return
        }
    }
}