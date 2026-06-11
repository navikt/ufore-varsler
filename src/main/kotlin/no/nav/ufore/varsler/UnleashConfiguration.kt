package no.nav.ufore.varsler

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class UnleashConfiguration {

    @Profile("!local")
    @Bean
    fun unleash(
        @Value("\${app.unleash.url}") unleashUrl: String,
        @Value("\${app.unleash.token}") unleashToken: String,
        @Value("\${spring.application.name}") appName: String,
    ): Unleash {
        val config = UnleashConfig.builder()
            .appName(appName)
            .instanceId(appName)
            .unleashAPI(unleashUrl)
            .apiKey(unleashToken)
            .synchronousFetchOnInitialisation(false)
            .build()
        return DefaultUnleash(config)
    }

    @Profile("local")
    @Bean
    fun lokalUnleash(): FakeUnleash {
        return FakeUnleash().apply {
            enableAll()
        }
    }
}
