package nl.ing.lovebird.tokens.authentication.sema;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.logging.SemaEvent;
import org.slf4j.Marker;

@RequiredArgsConstructor
public class ServiceRequestTokenParsingFailedSemaEvent implements SemaEvent {

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Marker getMarkers() {
        return null;
    }
}
