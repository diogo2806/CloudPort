package br.com.cloudport.servicogate.integration.alerta;

import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;

public class AlertaReconciliacaoBarcode {

    private final Long reconciliacaoId;
    private final String chaveIdempotencia;
    private final String tipo;
    private final String descricao;
    private final String codigoGatePass;
    private final String barcodeEsperado;
    private final String barcodeRecebido;
    private final String statusTos;
    private final String statusLocal;
    private final LocalDateTime detectadoEm;

    public AlertaReconciliacaoBarcode(ReconciliacaoBarcode reconciliacao) {
        this.reconciliacaoId = reconciliacao.getId();
        this.chaveIdempotencia = StringUtils.hasText(reconciliacao.getAlertaChaveIdempotencia())
                ? reconciliacao.getAlertaChaveIdempotencia()
                : "reconciliacao-barcode-" + reconciliacao.getId() + "-webhook";
        this.tipo = reconciliacao.getTipoDesinconia().name();
        this.descricao = reconciliacao.getDescricao();
        this.codigoGatePass = reconciliacao.getGatePass().getCodigo();
        this.barcodeEsperado = reconciliacao.getBarcodeEsperado();
        this.barcodeRecebido = reconciliacao.getBarcodeRecebido();
        this.statusTos = reconciliacao.getStatusTos();
        this.statusLocal = reconciliacao.getStatusLocal();
        this.detectadoEm = reconciliacao.getDetectadoEm();
    }

    public Long getReconciliacaoId() { return reconciliacaoId; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public String getCodigoGatePass() { return codigoGatePass; }
    public String getBarcodeEsperado() { return barcodeEsperado; }
    public String getBarcodeRecebido() { return barcodeRecebido; }
    public String getStatusTos() { return statusTos; }
    public String getStatusLocal() { return statusLocal; }
    public LocalDateTime getDetectadoEm() { return detectadoEm; }
}
