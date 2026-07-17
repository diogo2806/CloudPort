package br.com.cloudport.servicogate.integration.alerta;

import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeRepository;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusEntregaAlerta;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component
public class EntregaAlertaReconciliacaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntregaAlertaReconciliacaoService.class);
    private static final String CANAL_WEBHOOK = "WEBHOOK";

    private final ReconciliacaoBarcodeRepository reconciliacaoRepository;
    private final AlertaOperacionalGateway alertaOperacionalGateway;
    private final TransactionTemplate requiresNew;
    private final Duration alertaLease;

    public EntregaAlertaReconciliacaoService(
            ReconciliacaoBarcodeRepository reconciliacaoRepository,
            AlertaOperacionalGateway alertaOperacionalGateway,
            PlatformTransactionManager transactionManager,
            @Value("${gate.reconciliacao.alerta-lease:PT2M}") Duration alertaLease) {
        if (alertaLease == null || alertaLease.isZero() || alertaLease.isNegative()) {
            throw new IllegalArgumentException("O lease de alerta da reconciliação deve ser positivo.");
        }
        this.reconciliacaoRepository = reconciliacaoRepository;
        this.alertaOperacionalGateway = alertaOperacionalGateway;
        this.alertaLease = alertaLease;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int enviarAlertasPendentes() {
        List<Long> candidatos = reconciliacaoRepository
                .findByAlertaEnviadoFalseAndResolvidoEmIsNullOrderByDetectadoEmAsc()
                .stream()
                .map(ReconciliacaoBarcode::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int enviados = 0;
        int falhas = 0;
        for (Long reconciliacaoId : candidatos) {
            Optional<ReivindicacaoAlerta> reivindicacao = reivindicarAlertaPendente(reconciliacaoId);
            if (!reivindicacao.isPresent()) {
                continue;
            }

            ReivindicacaoAlerta alertaReivindicado = reivindicacao.get();
            try {
                ConfirmacaoEntregaAlerta confirmacao = alertaOperacionalGateway
                        .enviar(alertaReivindicado.getAlerta());
                if (confirmarEntrega(alertaReivindicado, confirmacao)) {
                    enviados++;
                    LOGGER.info(
                            "event=reconciliacao.alerta.confirmado id={} tentativa={} canal={} identificadorExterno={}",
                            alertaReivindicado.getReconciliacaoId(),
                            alertaReivindicado.getTentativa(),
                            confirmacao.getCanal(),
                            confirmacao.getIdentificadorExterno());
                } else {
                    falhas++;
                    LOGGER.warn(
                            "event=reconciliacao.alerta.confirmacao-ignorada id={} tentativa={} motivo=reivindicacao_substituida",
                            alertaReivindicado.getReconciliacaoId(),
                            alertaReivindicado.getTentativa());
                }
            } catch (RuntimeException ex) {
                registrarFalha(alertaReivindicado, ex.getMessage());
                falhas++;
                LOGGER.warn(
                        "event=reconciliacao.alerta.falha id={} tentativa={} cause={}",
                        alertaReivindicado.getReconciliacaoId(),
                        alertaReivindicado.getTentativa(),
                        ex.getMessage());
            }
        }

        if (falhas > 0) {
            throw new IllegalStateException(
                    "Falha ao entregar " + falhas + " alerta(s) da reconciliação de barcode.");
        }
        return enviados;
    }

    Optional<ReivindicacaoAlerta> reivindicarAlertaPendente(Long reconciliacaoId) {
        ReivindicacaoAlerta reivindicacao = requiresNew.execute(
                status -> reivindicarNaTransacao(reconciliacaoId));
        return Optional.ofNullable(reivindicacao);
    }

    boolean confirmarEntrega(
            ReivindicacaoAlerta reivindicacao,
            ConfirmacaoEntregaAlerta confirmacao) {
        Boolean confirmado = requiresNew.execute(
                status -> confirmarNaTransacao(reivindicacao, confirmacao));
        return Boolean.TRUE.equals(confirmado);
    }

    boolean registrarFalha(ReivindicacaoAlerta reivindicacao, String motivo) {
        Boolean registrado = requiresNew.execute(
                status -> registrarFalhaNaTransacao(reivindicacao, motivo));
        return Boolean.TRUE.equals(registrado);
    }

    private ReivindicacaoAlerta reivindicarNaTransacao(Long reconciliacaoId) {
        ReconciliacaoBarcode reconciliacao = reconciliacaoRepository
                .findOneById(reconciliacaoId)
                .orElse(null);
        if (reconciliacao == null) {
            return null;
        }

        LocalDateTime agora = agoraNormalizado();
        if (!estaElegivel(reconciliacao, agora)) {
            return null;
        }

        int tentativa = tentativas(reconciliacao) + 1;
        String token = UUID.randomUUID().toString();
        String chaveIdempotencia = reconciliacao.getAlertaChaveIdempotencia();
        if (!StringUtils.hasText(chaveIdempotencia)) {
            chaveIdempotencia = "reconciliacao-barcode-" + reconciliacao.getId() + "-webhook";
        }

        reconciliacao.setAlertaEnviado(false);
        reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.PROCESSANDO);
        reconciliacao.setAlertaCanal(CANAL_WEBHOOK);
        reconciliacao.setAlertaTentativas(tentativa);
        reconciliacao.setAlertaUltimoErro(null);
        reconciliacao.setAlertaChaveIdempotencia(chaveIdempotencia);
        reconciliacao.setAlertaReivindicacaoToken(token);
        reconciliacao.setAlertaReivindicadoEm(agora);
        reconciliacao.setAlertaLeaseAte(agora.plus(alertaLease));
        reconciliacaoRepository.saveAndFlush(reconciliacao);

        return new ReivindicacaoAlerta(
                reconciliacao.getId(),
                token,
                tentativa,
                new AlertaReconciliacaoBarcode(reconciliacao));
    }

    private boolean confirmarNaTransacao(
            ReivindicacaoAlerta reivindicacao,
            ConfirmacaoEntregaAlerta confirmacao) {
        if (reivindicacao == null || confirmacao == null) {
            return false;
        }

        ReconciliacaoBarcode reconciliacao = reconciliacaoRepository
                .findOneById(reivindicacao.getReconciliacaoId())
                .orElse(null);
        if (!correspondeReivindicacaoAtual(reconciliacao, reivindicacao)) {
            return false;
        }

        reconciliacao.setAlertaEnviado(true);
        reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.ENVIADO);
        reconciliacao.setAlertaEnviadoEm(
                confirmacao.getConfirmadoEm() == null ? agoraNormalizado() : confirmacao.getConfirmadoEm());
        reconciliacao.setAlertaCanal(confirmacao.getCanal());
        reconciliacao.setAlertaIdentificadorExterno(confirmacao.getIdentificadorExterno());
        reconciliacao.setAlertaUltimoErro(null);
        limparReivindicacao(reconciliacao);
        reconciliacaoRepository.saveAndFlush(reconciliacao);
        return true;
    }

    private boolean registrarFalhaNaTransacao(
            ReivindicacaoAlerta reivindicacao,
            String motivo) {
        if (reivindicacao == null) {
            return false;
        }

        ReconciliacaoBarcode reconciliacao = reconciliacaoRepository
                .findOneById(reivindicacao.getReconciliacaoId())
                .orElse(null);
        if (!correspondeReivindicacaoAtual(reconciliacao, reivindicacao)) {
            return false;
        }

        reconciliacao.setAlertaEnviado(false);
        reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.FALHA);
        reconciliacao.setAlertaUltimoErro(limitar(
                StringUtils.hasText(motivo) ? motivo : "Falha sem mensagem do provedor.",
                1000));
        limparReivindicacao(reconciliacao);
        reconciliacaoRepository.saveAndFlush(reconciliacao);
        return true;
    }

    private boolean estaElegivel(ReconciliacaoBarcode reconciliacao, LocalDateTime agora) {
        if (reconciliacao.isAlertaEnviado() || reconciliacao.getResolvidoEm() != null) {
            return false;
        }

        StatusEntregaAlerta status = reconciliacao.getStatusEntregaAlerta();
        if (status == null
                || status == StatusEntregaAlerta.PENDENTE
                || status == StatusEntregaAlerta.FALHA) {
            return true;
        }
        if (status == StatusEntregaAlerta.PROCESSANDO) {
            LocalDateTime leaseAte = reconciliacao.getAlertaLeaseAte();
            return leaseAte == null || !leaseAte.isAfter(agora);
        }
        return false;
    }

    private boolean correspondeReivindicacaoAtual(
            ReconciliacaoBarcode reconciliacao,
            ReivindicacaoAlerta reivindicacao) {
        return reconciliacao != null
                && reconciliacao.getStatusEntregaAlerta() == StatusEntregaAlerta.PROCESSANDO
                && Objects.equals(
                        reconciliacao.getAlertaReivindicacaoToken(),
                        reivindicacao.getToken());
    }

    private void limparReivindicacao(ReconciliacaoBarcode reconciliacao) {
        reconciliacao.setAlertaReivindicacaoToken(null);
        reconciliacao.setAlertaReivindicadoEm(null);
        reconciliacao.setAlertaLeaseAte(null);
    }

    private int tentativas(ReconciliacaoBarcode reconciliacao) {
        return reconciliacao.getAlertaTentativas() == null ? 0 : reconciliacao.getAlertaTentativas();
    }

    private String limitar(String valor, int tamanhoMaximo) {
        if (valor == null || valor.length() <= tamanhoMaximo) {
            return valor;
        }
        return valor.substring(0, tamanhoMaximo);
    }

    private LocalDateTime agoraNormalizado() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    static final class ReivindicacaoAlerta {

        private final Long reconciliacaoId;
        private final String token;
        private final int tentativa;
        private final AlertaReconciliacaoBarcode alerta;

        private ReivindicacaoAlerta(
                Long reconciliacaoId,
                String token,
                int tentativa,
                AlertaReconciliacaoBarcode alerta) {
            this.reconciliacaoId = reconciliacaoId;
            this.token = token;
            this.tentativa = tentativa;
            this.alerta = alerta;
        }

        Long getReconciliacaoId() {
            return reconciliacaoId;
        }

        String getToken() {
            return token;
        }

        int getTentativa() {
            return tentativa;
        }

        AlertaReconciliacaoBarcode getAlerta() {
            return alerta;
        }
    }
}
