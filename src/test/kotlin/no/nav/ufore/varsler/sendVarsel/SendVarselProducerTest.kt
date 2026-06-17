package no.nav.ufore.varsler.sendVarsel

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import java.util.*

class SendVarselProducerTest {

    private val kafkaTemplate = mockk<KafkaTemplate<String, String>>()

    private val producer = SendVarselProducer(
        kafkaTemplate = kafkaTemplate,
        producerTopic = "test-topic",
        cluster = "local",
        namespace = "local",
        appnavn = "ufore-varsler",
        dineMuligheterUrl = "https://uforetrygd-selvbetjening-frontend-borger.intern.dev.nav.no/uforetrygd/selvbetjening/dine-muligheter",
    )

    @Test
    fun `sendBeskjed skal ikke feile`() {
        val request = SendVarselRequest(
            varselId = UUID.randomUUID().toString(),
            ident = "12345678901",
        )
        producer.sendBeskjed(request)
    }
}
