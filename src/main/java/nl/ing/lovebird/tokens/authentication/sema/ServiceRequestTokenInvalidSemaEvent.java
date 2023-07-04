package nl.ing.lovebird.tokens.authentication.sema;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.Markers;
import nl.ing.lovebird.logging.SemaEvent;
import org.slf4j.Marker;

@RequiredArgsConstructor
public class ServiceRequestTokenInvalidSemaEvent implements SemaEvent {

    private final String message;
    private final String issuer;
    private final String keyId;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Marker getMarkers() {
        return Markers.append("issuer", issuer)
                .and(Markers.append("keyId", keyId));
    }
}
