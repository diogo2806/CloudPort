package br.com.cloudport.servicoyard.patio.listatrabalho.modelo;

import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
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
@Table(name = "ordem_trabalho_patio")
public class OrdemTrabalhoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteiner_id")
    private ConteinerPatio conteiner;

    @Column(name = "codigo_conteiner", nullable = false, length = 30)
    private String codigoConteiner;

    @Column(name = "tipo_carga", length = 40)
    private String tipoCarga;

    @Column(name = "destino", nullable = false, length = 60)
    private String destino;

    @Column(name = "linha_destino", nullable = false)
    private Integer linhaDestino;

    @Column(name = "coluna_destino", nullable = false)
    private Integer colunaDestino;

    @Column(name = "camada_destino", nullable = false, length = 40)
    private String camadaDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 30)
    private TipoMovimentoPatio tipoMovimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_ordem", nullable = false, length = 20)
    private StatusOrdemTrabalhoPatio statusOrdem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conteiner_destino", nullable = false, length = 30)
    private StatusConteiner statusConteinerDestino;

    @Column(name = "visita_navio_id")
    private Long visitaNavioId;

    @Column(name = "item_operacao_navio_id")
    private Long itemOperacaoNavioId;

    @Column(name = "plano_estiva_navio_id")
    private Long planoEstivaNavioId;

    @Column(name = "tipo_origem", length = 30)
    private String tipoOrigem;

    @Column(name = "tipo_destino", length = 30)
    private String tipoDestino;

    @Column(name = "sequencia_navio")
    private Integer sequenciaNavio;

    @Column(name = "prioridade_operacional")
    private Integer prioridadeOperacional;

    @Column(name = "prioridade_busca", nullable = false)
    private boolean prioridadeBusca;

    @Column(name = "work_queue_id")
    private Long workQueueId;

    @Column(name = "chave_idempotencia", length = 120, unique = true)
    private String chaveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_confirmacao_vmt", nullable = false, length = 30)
    private StatusConfirmacaoVmt statusConfirmacaoVmt = StatusConfirmacaoVmt.PENDENTE;

    @Column(name = "vmt_aceito_em")
    private LocalDateTime vmtAceitoEm;

    @Column(name = "vmt_iniciado_em")
    private LocalDateTime vmtIniciadoEm;

    @Column(name = "vmt_falha_em")
    private LocalDateTime vmtFalhaEm;

    @Column(name = "vmt_concluido_em")
    private LocalDateTime vmtConcluidoEm;

    @Column(name = "ultimo_evento_vmt_id", length = 120)
    private String ultimoEventoVmtId;

    @Column(name = "resultado_vmt", length = 1000)
    private String resultadoVmt;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    public OrdemTrabalhoPatio() {
    }

    public OrdemTrabalhoPatio(ConteinerPatio conteiner,
                               String codigoConteiner,
                               String tipoCarga,
                               String destino,
                               Integer linhaDestino,
                               Integer colunaDestino,
                               String camadaDestino,
                               TipoMovimentoPatio tipoMovimento,
                               StatusOrdemTrabalhoPatio statusOrdem,
                               StatusConteiner statusConteinerDestino,
                               LocalDateTime criadoEm,
                               LocalDateTime atualizadoEm) {
        this.conteiner = conteiner;
        this.codigoConteiner = codigoConteiner;
        this.tipoCarga = tipoCarga;
        this.destino = destino;
        this.linhaDestino = linhaDestino;
        this.colunaDestino = colunaDestino;
        this.camadaDestino = camadaDestino;
        this.tipoMovimento = tipoMovimento;
        this.statusOrdem = statusOrdem;
        this.statusConteinerDestino = statusConteinerDestino;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    private void reiniciarConfirmacaoVmt() {
        statusConfirmacaoVmt = StatusConfirmacaoVmt.PENDENTE;
        vmtAceitoEm = null;
        vmtIniciadoEm = null;
        vmtFalhaEm = null;
        vmtConcluidoEm = null;
        ultimoEventoVmtId = null;
        resultadoVmt = null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConteinerPatio getConteiner() { return conteiner; }
    public void setConteiner(ConteinerPatio conteiner) { this.conteiner = conteiner; }
    public String getCodigoConteiner() { return codigoConteiner; }
    public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
    public String getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public Integer getLinhaDestino() { return linhaDestino; }
    public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
    public Integer getColunaDestino() { return colunaDestino; }
    public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
    public String getCamadaDestino() { return camadaDestino; }
    public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
    public TipoMovimentoPatio getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) { this.tipoMovimento = tipoMovimento; }
    public StatusOrdemTrabalhoPatio getStatusOrdem() { return statusOrdem; }
    public void setStatusOrdem(StatusOrdemTrabalhoPatio statusOrdem) {
        if (statusOrdem == StatusOrdemTrabalhoPatio.PENDENTE
                && this.statusOrdem != null
                && this.statusOrdem != StatusOrdemTrabalhoPatio.PENDENTE) {
            reiniciarConfirmacaoVmt();
        }
        this.statusOrdem = statusOrdem;
    }
    public StatusConteiner getStatusConteinerDestino() { return statusConteinerDestino; }
    public void setStatusConteinerDestino(StatusConteiner statusConteinerDestino) { this.statusConteinerDestino = statusConteinerDestino; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
    public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
    public Long getPlanoEstivaNavioId() { return planoEstivaNavioId; }
    public void setPlanoEstivaNavioId(Long planoEstivaNavioId) { this.planoEstivaNavioId = planoEstivaNavioId; }
    public String getTipoOrigem() { return tipoOrigem; }
    public void setTipoOrigem(String tipoOrigem) { this.tipoOrigem = tipoOrigem; }
    public String getTipoDestino() { return tipoDestino; }
    public void setTipoDestino(String tipoDestino) { this.tipoDestino = tipoDestino; }
    public Integer getSequenciaNavio() { return sequenciaNavio; }
    public void setSequenciaNavio(Integer sequenciaNavio) { this.sequenciaNavio = sequenciaNavio; }
    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    public boolean isPrioridadeBusca() { return prioridadeBusca; }
    public void setPrioridadeBusca(boolean prioridadeBusca) { this.prioridadeBusca = prioridadeBusca; }
    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public StatusConfirmacaoVmt getStatusConfirmacaoVmt() { return statusConfirmacaoVmt; }
    public void setStatusConfirmacaoVmt(StatusConfirmacaoVmt statusConfirmacaoVmt) { this.statusConfirmacaoVmt = statusConfirmacaoVmt; }
    public LocalDateTime getVmtAceitoEm() { return vmtAceitoEm; }
    public void setVmtAceitoEm(LocalDateTime vmtAceitoEm) { this.vmtAceitoEm = vmtAceitoEm; }
    public LocalDateTime getVmtIniciadoEm() { return vmtIniciadoEm; }
    public void setVmtIniciadoEm(LocalDateTime vmtIniciadoEm) { this.vmtIniciadoEm = vmtIniciadoEm; }
    public LocalDateTime getVmtFalhaEm() { return vmtFalhaEm; }
    public void setVmtFalhaEm(LocalDateTime vmtFalhaEm) { this.vmtFalhaEm = vmtFalhaEm; }
    public LocalDateTime getVmtConcluidoEm() { return vmtConcluidoEm; }
    public void setVmtConcluidoEm(LocalDateTime vmtConcluidoEm) { this.vmtConcluidoEm = vmtConcluidoEm; }
    public String getUltimoEventoVmtId() { return ultimoEventoVmtId; }
    public void setUltimoEventoVmtId(String ultimoEventoVmtId) { this.ultimoEventoVmtId = ultimoEventoVmtId; }
    public String getResultadoVmt() { return resultadoVmt; }
    public void setResultadoVmt(String resultadoVmt) { this.resultadoVmt = resultadoVmt; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
}
