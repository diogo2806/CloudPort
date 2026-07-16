package br.com.cloudport.servicogate.scheduler;

import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeRepository;
import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeService;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliacaoBarcodeScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacaoBarcodeScheduler.class);

    private final ReconciliacaoBarcodeService reconciliacaoService;
    private final ReconciliacaoBarcodeRepository reconciliacaoRepository;

    public ReconciliacaoBarcodeScheduler(ReconciliacaoBarcodeService reconciliacaoService,
                                         ReconciliacaoBarcodeRepository reconciliacaoRepository) {
        this.reconciliacaoService = reconciliacaoService;
        this.reconciliacaoRepository = reconciliacaoRepository;
    }

    @Scheduled(cron = "${gate.reconciliacao.cron:0 0 2 * * *}")
    public void executarReconciliacaoNocturna() {
        try {
            LOGGER.info("event=reconciliacao.scheduler.iniciada timestamp={}", LocalDateTime.now());
            List<ReconciliacaoBarcode> problemas = reconciliacaoService.executarReconciliacao();
            enviarAlertas(problemas);
            LOGGER.info("event=reconciliacao.scheduler.concluida problemas={} timestamp={}",
                    problemas.size(), LocalDateTime.now());
        } catch (Exception ex) {
            LOGGER.error("event=reconciliacao.scheduler.erro cause={} timestamp={}",
                    ex.getMessage(), LocalDateTime.now(), ex);
        }
    }

    private void enviarAlertas(List<ReconciliacaoBarcode> problemas) {
        List<ReconciliacaoBarcode> naResolvidos = reconciliacaoRepository.findNaoResolvidosSemAlerta();
        for (ReconciliacaoBarcode reconciliacao : naResolvidos) {
            try {
                enviarAlerta(reconciliacao);
                reconciliacao.setAlertaEnviado(true);
                reconciliacaoRepository.save(reconciliacao);
            } catch (Exception ex) {
                LOGGER.warn("event=reconciliacao.alerta.erro id={} cause={} timestamp={}",
                        reconciliacao.getId(), ex.getMessage(), LocalDateTime.now());
            }
        }
    }

    private void enviarAlerta(ReconciliacaoBarcode reconciliacao) {
        String assunto = String.format("[ALERTA] Desincronização de Barcode: %s",
                reconciliacao.getTipoDesinconia().getDescricao());

        String mensagem = String.format(
                "ID: %d%n" +
                "Tipo: %s%n" +
                "Descrição: %s%n" +
                "Gate Pass: %s%n" +
                "Detectado em: %s%n" +
                "Tempo pendência: %d horas%n" +
                "Barcode Esperado: %s%n" +
                "Barcode Recebido: %s%n" +
                "Status TOS: %s%n" +
                "Status Local: %s%n",
                reconciliacao.getId(),
                reconciliacao.getTipoDesinconia().getDescricao(),
                reconciliacao.getDescricao(),
                reconciliacao.getGatePass().getCodigo(),
                reconciliacao.getDetectadoEm(),
                reconciliacao.getTempoPendenciaHoras() != null ? reconciliacao.getTempoPendenciaHoras() : 0,
                reconciliacao.getBarcodeEsperado() != null ? reconciliacao.getBarcodeEsperado() : "N/A",
                reconciliacao.getBarcodeRecebido() != null ? reconciliacao.getBarcodeRecebido() : "N/A",
                reconciliacao.getStatusTos() != null ? reconciliacao.getStatusTos() : "N/A",
                reconciliacao.getStatusLocal() != null ? reconciliacao.getStatusLocal() : "N/A"
        );

        LOGGER.warn("event=reconciliacao.alerta.enviado id={} tipo={} gatePass={} assunto={} mensagem={} timestamp={}",
                reconciliacao.getId(), reconciliacao.getTipoDesinconia(),
                reconciliacao.getGatePass().getCodigo(), assunto, mensagem, LocalDateTime.now());
    }
}
