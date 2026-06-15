package no.nav.ufore.varsler

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import org.springframework.util.backoff.FixedBackOff.DEFAULT_INTERVAL
import org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS

@Configuration
class KafkaConfig {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun errorHandler(): DefaultErrorHandler {
        // Prøver å lese feilende Kafka-meldinger uendelig mange ganger med 5 sek intervall
        return DefaultErrorHandler(FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)).also {
            it.setRetryListeners({ record, exception, deliveryAttempt ->
                logger.error("Kafkamelding feilet (forsøk $deliveryAttempt), partition=${record.partition()}, topic=${record.topic()}, offset=${record.offset()}", exception)
            })
        }
    }
}
