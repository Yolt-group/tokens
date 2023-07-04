package nl.ing.lovebird.tokens.authentication.sema;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.Markers;
import nl.ing.lovebird.logging.SemaEvent;
import org.slf4j.Marker;

import java.util.UUID;

@RequiredArgsConstructor
@AllArgsConstructor
public class ClientRequestTokenInvalidSemaEvent implements SemaEvent {

    private final String message;
    private final String keyId;
    private UUID clientId;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Marker getMarkers() {
        return Markers.append("clientId", clientId == null ? "unknown" : clientId.toString())
                .and(Markers.append("keyId", keyId));
    }
}
