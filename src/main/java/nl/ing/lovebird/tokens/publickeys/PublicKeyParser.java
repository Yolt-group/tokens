package nl.ing.lovebird.tokens.publickeys;

import nl.ing.lovebird.tokens.exception.InvalidPublicKeyException;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PublicKeyParser {

    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    private PublicKeyParser() {
    }

    public static PublicKey parse(@NotNull String publicKey) {
        try {
            String rawPublicKey = publicKey.substring(BEGIN_PUBLIC_KEY.length(), publicKey.indexOf(END_PUBLIC_KEY));
            String publicKeyWithoutSpaces = StringUtils.deleteWhitespace(rawPublicKey);
            byte[] byteKey = Base64.getDecoder().decode(publicKeyWithoutSpaces.getBytes());
            X509EncodedKeySpec x509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(x509publicKey);
        } catch (Exception e) {
            throw new InvalidPublicKeyException("Could not generate public key from " + publicKey, e);
        }
    }
}
