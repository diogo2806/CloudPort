package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.StatusEntregaAlerta;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "reconciliacao_barcode")
public class ReconciliacaoBarcode extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_pass_id", nullable = false)
    private GatePass gatePass;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_desinconia", nullable = false, length = 40)
    private TipoDesincroniaBarcode tipoDesinconia;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "barcode_esperado", length = 50)
    private String barcodeEsperado;

    @Column(name = "barcode_recebido", length = 50)
    private String barcodeRecebido;

    @Column(name = "status_tos", length = 50)
    private String statusTos;

    @Column(name = "status_local", length = 50)
    private String statusLocal;

    @Column(name = "tempo_pendencia_horas")
    private Integer tempoPendenciaHoras;

    @Column(name = "detectado_em", nullable = false)
    private LocalDateTime detectadoEm;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "resolucao", length = 500)
    private String resolucao;

    @Column(name = "alerta_enviado", nullable = false)
    private boolean alertaEnviado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_entrega_alerta", nullable = false, length = 20)
    private StatusEntregaAlerta statusEntregaAlerta = StatusEntregaAlerta.PENDENTE;

    @Column(name = "alerta_enviado_em")
    private LocalDateTime alertaEnviadoEm;

    @Column(name = "alerta_canal", length = 40)
    private String alertaCanal;

    @Column(name = "alerta_identificador_externo", length = 120)
    private String alertaIdentificadorExterno;

    @Column(name = "alerta_tentativas", nullable = false)
    private Integer alertaTentativas = 0;

    @Column(name = "alerta_ultimo_erro", length = 1000)
    private String alertaUltimoErro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GatePass getGatePass() { return gatePass; }
    public void setGatePass(GatePass gatePass) { this.gatePass = gatePass; }
    public TipoDesincroniaBarcode getTipoDesinconia() { return tipoDesinconia; }
    public void setTipoDesinconia(TipoDesincroniaBarcode tipoDesinconia) { this.tipoDesinconia = tipoDesinconia; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getBarcodeEsperado() { return barcodeEsperado; }
    public void setBarcodeEsperado(String barcodeEsperado) { this.barcodeEsperado = barcodeEsperado; }
    public String getBarcodeRecebido() { return barcodeRecebido; }
    public void setBarcodeRecebido(String barcodeRecebido) { this.barcodeRecebido = barcodeRecebido; }
    public String getStatusTos() { return statusTos; }
    public void setStatusTos(String statusTos) { this.statusTos = statusTos; }
    public String getStatusLocal() { return statusLocal; }
    public void setStatusLocal(String statusLocal) { this.statusLocal = statusLocal; }
    public Integer getTempoPendenciaHoras() { return tempoPendenciaHoras; }
    public void setTempoPendenciaHoras(Integer tempoPendenciaHoras) { this.tempoPendenciaHoras = tempoPendenciaHoras; }
    public LocalDateTime getDetectadoEm() { return detectadoEm; }
    public void setDetectadoEm(LocalDateTime detectadoEm) { this.detectadoEm = detectadoEm; }
    public LocalDateTime getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(LocalDateTime resolvidoEm) { this.resolvidoEm = resolvidoEm; }
    public String getResolucao() { return resolucao; }
    public void setResolucao(String resolucao) { this.resolucao = resolucao; }
    public boolean isAlertaEnviado() { return alertaEnviado; }
    public void setAlertaEnviado(boolean alertaEnviado) { this.alertaEnviado = alertaEnviado; }
    public StatusEntregaAlerta getStatusEntregaAlerta() { return statusEntregaAlerta; }
    public void setStatusEntregaAlerta(StatusEntregaAlerta statusEntregaAlerta) { this.statusEntregaAlerta = statusEntregaAlerta; }
    public LocalDateTime getAlertaEnviadoEm() { return alertaEnviadoEm; }
    public void setAlertaEnviadoEm(LocalDateTime alertaEnviadoEm) { this.alertaEnviadoEm = alertaEnviadoEm; }
    public String getAlertaCanal() { return alertaCanal; }
    public void setAlertaCanal(String alertaCanal) { this.alertaCanal = alertaCanal; }
    public String getAlertaIdentificadorExterno() { return alertaIdentificadorExterno; }
    public void setAlertaIdentificadorExterno(String alertaIdentificadorExterno) { this.alertaIdentificadorExterno = alertaIdentificadorExterno; }
    public Integer getAlertaTentativas() { return alertaTentativas; }
    public void setAlertaTentativas(Integer alertaTentativas) { this.alertaTentativas = alertaTentativas; }
    public String getAlertaUltimoErro() { return alertaUltimoErro; }
    public void setAlertaUltimoErro(String alertaUltimoErro) { this.alertaUltimoErro = alertaUltimoErro; }
}
