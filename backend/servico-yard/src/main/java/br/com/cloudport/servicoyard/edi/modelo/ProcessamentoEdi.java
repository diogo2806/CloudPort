package br.com.cloudport.servicoyard.edi.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "edi_processamento")
public class ProcessamentoEdi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mensagem", nullable = false, length = 20)
    private TipoMensagemEdi tipoMensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusProcessamentoEdi status;

    @Column(name = "conteudo_original", nullable = false, columnDefinition = "TEXT")
    private String conteudoOriginal;

    @Column(name = "identificador_unb", length = 100)
    private String identificadorUnb;

    @Column(name = "identificador_unh", length = 100)
    private String identificadorUnh;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 64)
    private String chaveIdempotencia;

    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    @Column(name = "codigo_navio", length = 50)
    private String codigoNavio;

    @Column(name = "codigo_viagem", length = 30)
    private String codigoViagem;

    @Column(name = "referencia_mensagem", length = 100)
    private String referenciaMensagem;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "motivo_rejeicao", length = 2000)
    private String motivoRejeicao;

    @Column(name = "motivo_reprocessamento", length = 500)
    private String motivoReprocessamento;

    @Column(name = "usuario_reprocessamento", length = 150)
    private String usuarioReprocessamento;

    @Column(name = "reprocessamento_de_id")
    private Long reprocessamentoDeId;

    @Column(name = "tentativa", nullable = false)
    private Integer tentativa;

    @Column(name = "proxima_tentativa_em")
    private LocalDateTime proximaTentativaEm;

    @Column(name = "processando_desde")
    private LocalDateTime processandoDesde;

    @Column(name = "bay_plan_id")
    private Long bayPlanId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        if (tentativa == null) {
            tentativa = 1;
        }
        if (status == null) {
            status = StatusProcessamentoEdi.RECEBIDO;
        }
        if (proximaTentativaEm == null && status == StatusProcessamentoEdi.RECEBIDO) {
            proximaTentativaEm = agora;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public TipoMensagemEdi getTipoMensagem() { return tipoMensagem; }
    public void setTipoMensagem(TipoMensagemEdi tipoMensagem) { this.tipoMensagem = tipoMensagem; }
    public StatusProcessamentoEdi getStatus() { return status; }
    public void setStatus(StatusProcessamentoEdi status) { this.status = status; }
    public String getConteudoOriginal() { return conteudoOriginal; }
    public void setConteudoOriginal(String conteudoOriginal) { this.conteudoOriginal = conteudoOriginal; }
    public String getIdentificadorUnb() { return identificadorUnb; }
    public void setIdentificadorUnb(String identificadorUnb) { this.identificadorUnb = identificadorUnb; }
    public String getIdentificadorUnh() { return identificadorUnh; }
    public void setIdentificadorUnh(String identificadorUnh) { this.identificadorUnh = identificadorUnh; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public String getHashConteudo() { return hashConteudo; }
    public void setHashConteudo(String hashConteudo) { this.hashConteudo = hashConteudo; }
    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getReferenciaMensagem() { return referenciaMensagem; }
    public void setReferenciaMensagem(String referenciaMensagem) { this.referenciaMensagem = referenciaMensagem; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getMotivoRejeicao() { return motivoRejeicao; }
    public void setMotivoRejeicao(String motivoRejeicao) { this.motivoRejeicao = motivoRejeicao; }
    public String getMotivoReprocessamento() { return motivoReprocessamento; }
    public void setMotivoReprocessamento(String motivoReprocessamento) { this.motivoReprocessamento = motivoReprocessamento; }
    public String getUsuarioReprocessamento() { return usuarioReprocessamento; }
    public void setUsuarioReprocessamento(String usuarioReprocessamento) { this.usuarioReprocessamento = usuarioReprocessamento; }
    public Long getReprocessamentoDeId() { return reprocessamentoDeId; }
    public void setReprocessamentoDeId(Long reprocessamentoDeId) { this.reprocessamentoDeId = reprocessamentoDeId; }
    public Integer getTentativa() { return tentativa; }
    public void setTentativa(Integer tentativa) { this.tentativa = tentativa; }
    public LocalDateTime getProximaTentativaEm() { return proximaTentativaEm; }
    public void setProximaTentativaEm(LocalDateTime proximaTentativaEm) { this.proximaTentativaEm = proximaTentativaEm; }
    public LocalDateTime getProcessandoDesde() { return processandoDesde; }
    public void setProcessandoDesde(LocalDateTime processandoDesde) { this.processandoDesde = processandoDesde; }
    public Long getBayPlanId() { return bayPlanId; }
    public void setBayPlanId(Long bayPlanId) { this.bayPlanId = bayPlanId; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
