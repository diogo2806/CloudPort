package br.com.cloudport.servicogate.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class OcrAsyncConfig {

    @Bean("ocrTaskExecutor")
    public Executor ocrTaskExecutor(
            @Value("${cloudport.gate.ocr.executor.core-size:2}") int coreSize,
            @Value("${cloudport.gate.ocr.executor.max-size:4}") int maxSize,
            @Value("${cloudport.gate.ocr.executor.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("cloudport-ocr-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
