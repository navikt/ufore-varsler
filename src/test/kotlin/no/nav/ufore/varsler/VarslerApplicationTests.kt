package no.nav.ufore.varsler

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.kafka.listener.auto-startup=false"])
class VarslerApplicationTests {

	@Test
	fun contextLoads() {
	}

}
