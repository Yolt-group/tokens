package nl.ing.lovebird.tokens.authentication;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticationAttemptMetrics {

    private static final String CLIENT_LABEL = "client_id";
    private static final String JWS_ALGORITHM_LABEL = "jws_algorithm";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Counter AUTHENTICATION_ATTEMPTS_SUCCESS = Counter.build()
            .labelNames(CLIENT_LABEL, JWS_ALGORITHM_LABEL)
            .name("authentication_attempts_success_total")
            .help("counts the successful authentication attempts per client.")
            .register();

    private static final Counter AUTHENTICATION_ATTEMPTS_ERROR = Counter.build()
            .labelNames(CLIENT_LABEL, JWS_ALGORITHM_LABEL)
            .name("authentication_attempts_error_total")
            .help("counts the unsuccessful authentication attempts per client.")
            .register();

    @Autowired
    private AuthenticationAttemptMetrics(final PrometheusMeterRegistry registry) {
        registry.getPrometheusRegistry().register(AUTHENTICATION_ATTEMPTS_SUCCESS);
        registry.getPrometheusRegistry().register(AUTHENTICATION_ATTEMPTS_ERROR);
    }

    static void incrementSuccessFullAuthentication(UUID clientUuid, String jwsAlgorithm) {
        AUTHENTICATION_ATTEMPTS_SUCCESS.labels(clientUuid.toString(), jwsAlgorithm).inc();
    }

    static void incrementErrorAuthentication(UUID clientUuid, String jwsAlgorithm) {
        AUTHENTICATION_ATTEMPTS_ERROR.labels(clientUuid.toString(), jwsAlgorithm).inc();
    }

    static void incrementErrorAuthentication(String jwsAlgorithm) {
        AUTHENTICATION_ATTEMPTS_ERROR.labels(UNKNOWN, jwsAlgorithm).inc();
    }

    static void incrementErrorAuthentication() {
        AUTHENTICATION_ATTEMPTS_ERROR.labels(UNKNOWN, UNKNOWN).inc();
    }
}
