package no.nav.ufore.varsler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import org.springframework.util.backoff.FixedBackOff.DEFAULT_INTERVAL
import org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS

@Configuration
class KafkaConfig {

    @Bean
    fun errorHandler(): DefaultErrorHandler {
        // Prøver å lese feilende Kafka-meldinger uendelig mange ganger med 5 sek intervall
        return DefaultErrorHandler(FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS))
    }
}