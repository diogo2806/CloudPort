package br.com.cloudport.servicogate.scheduler;

import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeService;
import br.com.cloudport.servicogate.integration.alerta.EntregaAlertaReconciliacaoService;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReconciliacaoBarcodeJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacaoBarcodeJob.class);

    private final ReconciliacaoBarcodeService reconciliacaoService;
    private final EntregaAlertaReconciliacaoService entregaAlertaService;
    private final ReconciliacaoBarcodeCicloCoordenador cicloCoordenador;

    public ReconciliacaoBarcodeJob(
            ReconciliacaoBarcodeService reconciliacaoService,
            EntregaAlertaReconciliacaoService entregaAlertaService,
            ReconciliacaoBarcodeCicloCoordenador cicloCoordenador) {
        this.reconciliacaoService = reconciliacaoService;
        this.entregaAlertaService = entregaAlertaService;
        this.cicloCoordenador = cicloCoordenador;
    }

    public void executar() {
        Optional<ReconciliacaoBarcodeCicloCoordenador.ReivindicacaoCiclo> reivindicacao =
                cicloCoordenador.reivindicar();
        if (!reivindicacao.isPresent()) {
            LOGGER.debug("event=reconciliacao.scheduler.ignorada motivo=ciclo_reivindicado_por_outra_instancia");
            return;
        }

        try (ReconciliacaoBarcodeCicloCoordenador.ReivindicacaoCiclo ciclo = reivindicacao.get()) {
            LOGGER.info("event=reconciliacao.scheduler.iniciada timestamp={}", LocalDateTime.now());
            List<ReconciliacaoBarcode> problemas = reconciliacaoService.executarReconciliacao();
            int alertasEnviados = entregaAlertaService.enviarAlertasPendentes();
            LOGGER.info(
                    "event=reconciliacao.scheduler.concluida problemas={} alertasEnviados={} timestamp={}",
                    problemas.size(),
                    alertasEnviados,
                    LocalDateTime.now());
        }
    }
}
