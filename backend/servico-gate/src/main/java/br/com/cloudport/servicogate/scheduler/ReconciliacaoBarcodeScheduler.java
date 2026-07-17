package br.com.cloudport.servicogate.scheduler;

import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeRepository;
import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeService;
import br.com.cloudport.servicogate.integration.alerta.AlertaOperacionalGateway;
import br.com.cloudport.servicogate.integration.alerta.AlertaReconciliacaoBarcode;
import br.com.cloudport.servicogate.integration.alerta.ConfirmacaoEntregaAlerta;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusEntregaAlerta;
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
    private final AlertaOperacionalGateway alertaOperacionalGateway;

    public ReconciliacaoBarcodeScheduler(ReconciliacaoBarcodeService reconciliacaoService,
                                         ReconciliacaoBarcodeRepository reconciliacaoRepository,
                                         AlertaOperacionalGateway alertaOperacionalGateway) {
        this.reconciliacaoService = reconciliacaoService;
        this.reconciliacaoRepository = reconciliacaoRepository;
        this.alertaOperacionalGateway = alertaOperacionalGateway;
    }

    @Scheduled(cron = "${gate.reconciliacao.cron:0 0 2 * * *}")
    public void executarReconciliacaoNocturna() {
        try {
            LOGGER.info("event=reconciliacao.scheduler.iniciada timestamp={}", LocalDateTime.now());
            List<ReconciliacaoBarcode> problemas = reconciliacaoService.executarReconciliacao();
            enviarAlertas();
            LOGGER.info("event=reconciliacao.scheduler.concluida problemas={} timestamp={}",
                    problemas.size(), LocalDateTime.now());
        } catch (Exception ex) {
            LOGGER.error("event=reconciliacao.scheduler.erro cause={} timestamp={}",
                    ex.getMessage(), LocalDateTime.now(), ex);
        }
    }

    private void enviarAlertas() {
        List<ReconciliacaoBarcode> naoResolvidos = reconciliacaoRepository.findNaoResolvidosSemAlerta();
        for (ReconciliacaoBarcode reconciliacao : naoResolvidos) {
            enviarAlerta(reconciliacao);
        }
    }

    private void enviarAlerta(ReconciliacaoBarcode reconciliacao) {
        try {
            int tentativas = reconciliacao.getAlertaTentativas() == null
                    ? 0
                    : reconciliacao.getAlertaTentativas();
            reconciliacao.setAlertaTentativas(tentativas + 1);
            reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.PENDENTE);
            reconciliacao.setAlertaUltimoErro(null);
            reconciliacaoRepository.saveAndFlush(reconciliacao);

            ConfirmacaoEntregaAlerta confirmacao = alertaOperacionalGateway
                    .enviar(new AlertaReconciliacaoBarcode(reconciliacao));
            reconciliacao.setAlertaEnviado(true);
            reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.ENVIADO);
            reconciliacao.setAlertaEnviadoEm(confirmacao.getConfirmadoEm());
            reconciliacao.setAlertaCanal(confirmacao.getCanal());
            reconciliacao.setAlertaIdentificadorExterno(confirmacao.getIdentificadorExterno());
            reconciliacao.setAlertaUltimoErro(null);
            reconciliacaoRepository.saveAndFlush(reconciliacao);
            LOGGER.info("event=reconciliacao.alerta.confirmado id={} canal={} identificadorExterno={}",
                    reconciliacao.getId(), confirmacao.getCanal(), confirmacao.getIdentificadorExterno());
        } catch (Exception ex) {
            reconciliacao.setAlertaEnviado(false);
            reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.FALHA);
            reconciliacao.setAlertaUltimoErro(limitar(ex.getMessage(), 1000));
            reconciliacaoRepository.saveAndFlush(reconciliacao);
            LOGGER.warn("event=reconciliacao.alerta.falha id={} tentativa={} cause={}",
                    reconciliacao.getId(), reconciliacao.getAlertaTentativas(), ex.getMessage());
        }
    }

    private String limitar(String valor, int tamanhoMaximo) {
        if (valor == null || valor.length() <= tamanhoMaximo) {
            return valor;
        }
        return valor.substring(0, tamanhoMaximo);
    }
}
