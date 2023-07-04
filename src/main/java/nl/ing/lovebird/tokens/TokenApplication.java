package nl.ing.lovebird.tokens;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.security.Security;
import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties
public class TokenApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenApplication.class, args);
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
