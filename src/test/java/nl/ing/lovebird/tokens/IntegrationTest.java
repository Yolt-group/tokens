package nl.ing.lovebird.tokens;

import nl.ing.lovebird.cassandra.test.EnableExternalCassandraTestDatabase;
import nl.ing.lovebird.kafka.test.EnableExternalKafkaTestCluster;
import nl.ing.lovebird.postgres.test.EnableExternalPostgresTestDatabase;
import nl.ing.lovebird.tokens.authentication.AuthenticationAttemptMetrics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.vault.core.VaultTemplate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@MockBean({AuthenticationAttemptMetrics.class})
@EnableExternalPostgresTestDatabase
@EnableExternalCassandraTestDatabase
@EnableExternalKafkaTestCluster
public @interface IntegrationTest {
    // Annotations only
}