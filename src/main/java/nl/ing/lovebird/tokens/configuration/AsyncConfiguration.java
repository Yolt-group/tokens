package nl.ing.lovebird.tokens.configuration;

import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@EnableAsync
public class AsyncConfiguration {

    @Bean
    public Executor checkVerificationKeysExecutor(TaskExecutorBuilder builder) {
        return builder
                .corePoolSize(1)
                .maxPoolSize(1)
                .queueCapacity(1)
                .threadNamePrefix("checkVerificationKeysExecutor-")
                .build();
    }
}
