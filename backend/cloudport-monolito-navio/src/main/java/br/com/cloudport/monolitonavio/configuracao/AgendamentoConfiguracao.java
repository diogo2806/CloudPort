package br.com.cloudport.monolitonavio.configuracao;

import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "cloudport.runtime.jobs-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AgendamentoConfiguracao implements SchedulingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgendamentoConfiguracao.class);

    private final int poolSize;

    public AgendamentoConfiguracao(
            @Value("${cloudport.runtime.scheduler.pool-size:8}") int poolSize) {
        this.poolSize = Math.max(1, poolSize);
    }

    @Bean(name = "cloudportTaskScheduler")
    public ThreadPoolTaskScheduler cloudportTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("cloudport-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setErrorHandler(erro -> LOGGER.error("Falha não tratada em job agendado", erro));
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(cloudportTaskScheduler());
    }
}
