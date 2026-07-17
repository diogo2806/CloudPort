package br.com.cloudport.servicogate.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cloudport.runtime",
        name = "jobs-enabled",
        havingValue = "true")
public class ReconciliacaoBarcodeScheduler {

    private final ReconciliacaoBarcodeJob reconciliacaoBarcodeJob;

    public ReconciliacaoBarcodeScheduler(ReconciliacaoBarcodeJob reconciliacaoBarcodeJob) {
        this.reconciliacaoBarcodeJob = reconciliacaoBarcodeJob;
    }

    @Scheduled(cron = "${gate.reconciliacao.cron:0 0 2 * * *}")
    public void executarReconciliacaoNocturna() {
        reconciliacaoBarcodeJob.executar();
    }
}
