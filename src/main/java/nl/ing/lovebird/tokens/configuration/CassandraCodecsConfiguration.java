package nl.ing.lovebird.tokens.configuration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * This config class makes sure all the generically used enums are only registered once, which prevents nasty overrides when not intended.
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class CassandraCodecsConfiguration {

    private final Cluster cluster;

    @PostConstruct
    public void onInit() {
        registerEnum(ServiceType.class);
    }

    private <E extends Enum<E>> void registerEnum(Class<E> clazz) {
        this.cluster.getConfiguration().getCodecRegistry().register(new EnumNameCodec<>(clazz));
    }
}
