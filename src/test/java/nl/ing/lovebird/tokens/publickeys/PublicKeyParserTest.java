package nl.ing.lovebird.tokens.publickeys;

import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.exception.InvalidPublicKeyException;
import org.junit.Test;

import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;

public class PublicKeyParserTest {

    @Test(expected = InvalidPublicKeyException.class)
    public void whenKeyFormatIncorrect_shouldThrowException() {
        PublicKeyParser.parse("incorrect_key");
    }

    @Test
    public void whenProperKey_shouldReturnIt() {
        PublicKey publicKey = PublicKeyParser.parse(TestUtil.getDefaultPublicKey());

        assertThat(publicKey).isNotNull();
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
    }
}
