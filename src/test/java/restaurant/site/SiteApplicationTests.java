package restaurant.site;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the whole application, so it needs a real MySQL and Redis and the variables from .env.
// Without them (a plain "mvn test", a fresh clone) it is skipped instead of failing.
// Run it from IntelliJ with the .env run configuration, or after the Testcontainers tests exist.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
class SiteApplicationTests {

    @Test
    void contextLoads() {
    }

}
