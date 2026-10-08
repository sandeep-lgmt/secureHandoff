package com.securehandoff.securehandoff;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Needs live MySQL, Redis and Kafka. Enable once Testcontainers (or local services) are set up.")
class SecurehandoffApplicationTests {

    @Test
    void contextLoads() {
    }
}
