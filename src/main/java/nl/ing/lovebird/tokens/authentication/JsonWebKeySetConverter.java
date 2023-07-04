package nl.ing.lovebird.tokens.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.logging.SemaEventLogger;
import nl.ing.lovebird.tokens.authentication.sema.PrivateComponentInPublicJWKSSemaEvent;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationPropertiesBinding
public class JsonWebKeySetConverter implements Converter<String, JsonWebKeySet> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public JsonWebKeySet convert(String source) {
        if (StringUtils.isEmpty(source)) {
            return null;
        }

        JSONObject jwksJSON = getJWKSAsJSON(source);
        try {
            return new JsonWebKeySet(jwksJSON.toJSONString());
        } catch (JoseException e) {
            throw new IllegalArgumentException("Failed parsing the JSON object into a JsonWebKeySet");
        }
    }

    private JSONObject getJWKSAsJSON(String jwksString) {
        try {
            JSONObject jwksJSON = MAPPER.readValue(jwksString, JSONObject.class);
            if (jwksJSON.toJSONString().contains("\"d\"")) {
                String message = "Found illegal private component \"d\" in jwks";
                SemaEventLogger.log(new PrivateComponentInPublicJWKSSemaEvent(message));
                throw new IllegalArgumentException(message);
            }
            return jwksJSON;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed parsing the JWKS value into a JSON Object", e);
        }
    }

}