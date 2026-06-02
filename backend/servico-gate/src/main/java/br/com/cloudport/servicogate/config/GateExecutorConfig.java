package br.com.cloudport.servicogate.config;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GateExecutorConfig {

    @Bean(name = "gateExecutorService")
    public ScheduledExecutorService gateExecutorService() {
        return Executors.newScheduledThreadPool(5);
    }
}
