package nl.ing.lovebird.tokens.authentication;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AuthenticationAttemptMetricsTest {

    @BeforeClass
    public static void setup() {
        getErrorCounter().clear();
        getSuccessCounter().clear();
    }

    @Test
    public void shouldIncrementCounters() {
        final UUID clientUuid = new UUID(0, 0);
        final UUID clientUuid2 = new UUID(1, 1);
        final UUID clientUuid3 = new UUID(2, 2);

        String jwsAlgo = "RS256";
        AuthenticationAttemptMetrics.incrementSuccessFullAuthentication(clientUuid, jwsAlgo);
        AuthenticationAttemptMetrics.incrementSuccessFullAuthentication(clientUuid, jwsAlgo);
        AuthenticationAttemptMetrics.incrementSuccessFullAuthentication(clientUuid2, jwsAlgo);
        AuthenticationAttemptMetrics.incrementSuccessFullAuthentication(clientUuid3, jwsAlgo);

        AuthenticationAttemptMetrics.incrementErrorAuthentication(clientUuid2, jwsAlgo);
        AuthenticationAttemptMetrics.incrementErrorAuthentication(clientUuid, jwsAlgo);
        AuthenticationAttemptMetrics.incrementErrorAuthentication(clientUuid3, jwsAlgo);
        AuthenticationAttemptMetrics.incrementErrorAuthentication(clientUuid3, jwsAlgo);

        assertSuccessRequestWasRecordedByPrometheus(new ExpectedCounterItem(clientUuid.toString(), jwsAlgo, 2),
                new ExpectedCounterItem(clientUuid2.toString(), jwsAlgo, 1),
                new ExpectedCounterItem(clientUuid3.toString(), jwsAlgo, 1));

        assertErrorRequestWasRecordedByPrometheus(new ExpectedCounterItem(clientUuid.toString(), jwsAlgo, 1),
                new ExpectedCounterItem(clientUuid2.toString(), jwsAlgo, 1),
                new ExpectedCounterItem(clientUuid3.toString(), jwsAlgo, 2));
    }

    public static void assertSuccessRequestWasRecordedByPrometheus(final ExpectedCounterItem... expectedCounterItems) {
        Counter authenticationAttemptsSuccess = getSuccessCounter();
        assertExpectedCounterItems(authenticationAttemptsSuccess, "authentication_attempts_success_total", expectedCounterItems);
    }

    public static void assertErrorRequestWasRecordedByPrometheus(final ExpectedCounterItem... expectedCounterItems) {
        Counter authenticationAttemptsError = getErrorCounter();
        assertExpectedCounterItems(authenticationAttemptsError, "authentication_attempts_error_total", expectedCounterItems);
    }

    private static Counter getSuccessCounter() {
        return (Counter) ReflectionTestUtils.getField(AuthenticationAttemptMetrics.class, "AUTHENTICATION_ATTEMPTS_SUCCESS");
    }

    private static Counter getErrorCounter() {
        return (Counter) ReflectionTestUtils.getField(AuthenticationAttemptMetrics.class, "AUTHENTICATION_ATTEMPTS_ERROR");
    }

    private static void assertExpectedCounterItems(Counter authenticationAttemptsSuccess, String counterName, ExpectedCounterItem[] expectedCounterItems) {
        List<Collector.MetricFamilySamples.Sample> samples = authenticationAttemptsSuccess.collect().get(0).samples.stream()
                .filter(sample -> sample.name.equals(counterName))
                .collect(Collectors.toList());

        assertEquals(expectedCounterItems.length, samples.size());

        for (ExpectedCounterItem expectedCounterItem : expectedCounterItems) {
            assertContains(samples, expectedCounterItem);
        }
    }

    private static void assertContains(List<Collector.MetricFamilySamples.Sample> samples, ExpectedCounterItem expectedCounterItem) {

        for (Collector.MetricFamilySamples.Sample sample : samples) {
            if (expectedCounterItem.getClientId().equals(sample.labelValues.get(0))) {
                assertEquals(expectedCounterItem.getCount(), sample.value, 0);
                return;
            }
        }
        assertFalse("could not find expected counter item in samples with client id: " + expectedCounterItem.getClientId(), true);
    }

    @Data
    @AllArgsConstructor
    public static class ExpectedCounterItem {
        private final String clientId;
        private final String jwsAlgo;
        private final double count;
    }
}