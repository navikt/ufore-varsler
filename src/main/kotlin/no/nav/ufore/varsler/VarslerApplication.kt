package no.nav.ufore.varsler

import org.springframework.kafka.annotation.EnableKafka
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableKafka
@SpringBootApplication
class VarslerApplication

fun main(args: Array<String>) {
	runApplication<VarslerApplication>(*args)
}
