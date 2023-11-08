package dev.cmartin.microserviceone.integration

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MicroserviceOneTestIT {

    @Test
    fun testFailsafePlugin() {
        assertTrue(true)
    }

}