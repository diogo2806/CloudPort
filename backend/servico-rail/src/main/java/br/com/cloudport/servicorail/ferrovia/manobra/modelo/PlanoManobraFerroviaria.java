package br.com.cloudport.servicorail.ferrovia.manobra.modelo;

import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "plano_manobra_ferroviaria",
        uniqueConstraints = @UniqueConstraint(name = "uk_plano_manobra_visita_sequencia",
                columnNames = {"visita_trem_id", "sequencia"}))
public class PlanoManobraFerroviaria {

    public enum StatusPlanoManobra {
        PLANEJADA,
        BLOQUEADA_CONFLITO,
        AUTORIZADA,
        EM_EXECUCAO,
        CONCLUIDA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_trem_id", nullable = false)
    private VisitaTrem visitaTrem;

    @Column(name = "sequencia", nullable = false)
    private Integer sequencia;

    @Column(name = "origem", nullable = false, length = 120)
    private String origem;

    @Column(name = "destino", nullable = false, length = 120)
    private String destino;

    @Column(name = "composicao", nullable = false, length = 200)
    private String composicao;

    @Column(name = "linha", nullable = false, length = 80)
    private String linha;

    @Column(name = "trecho", nullable = false, length = 120)
    private String trecho;

    @Column(name = "inicio_previsto", nullable = false)
    private LocalDateTime inicioPrevisto;

    @Column(name = "fim_previsto", nullable = false)
    private LocalDateTime fimPrevisto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPlanoManobra status = StatusPlanoManobra.PLANEJADA;

    @Column(name = "conflito_descricao", length = 500)
    private String conflitoDescricao;

    @Column(name = "autorizado_por", length = 120)
    private String autorizadoPor;

    @Column(name = "autorizado_em")
    private LocalDateTime autorizadoEm;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        if (this.status == null) {
            this.status = StatusPlanoManobra.PLANEJADA;
        }
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public VisitaTrem getVisitaTrem() {
        return visitaTrem;
    }

    public void setVisitaTrem(VisitaTrem visitaTrem) {
        this.visitaTrem = visitaTrem;
    }

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getComposicao() {
        return composicao;
    }

    public void setComposicao(String composicao) {
        this.composicao = composicao;
    }

    public String getLinha() {
        return linha;
    }

    public void setLinha(String linha) {
        this.linha = linha;
    }

    public String getTrecho() {
        return trecho;
    }

    public void setTrecho(String trecho) {
        this.trecho = trecho;
    }

    public LocalDateTime getInicioPrevisto() {
        return inicioPrevisto;
    }

    public void setInicioPrevisto(LocalDateTime inicioPrevisto) {
        this.inicioPrevisto = inicioPrevisto;
    }

    public LocalDateTime getFimPrevisto() {
        return fimPrevisto;
    }

    public void setFimPrevisto(LocalDateTime fimPrevisto) {
        this.fimPrevisto = fimPrevisto;
    }

    public StatusPlanoManobra getStatus() {
        return status;
    }

    public void setStatus(StatusPlanoManobra status) {
        this.status = status;
    }

    public String getConflitoDescricao() {
        return conflitoDescricao;
    }

    public void setConflitoDescricao(String conflitoDescricao) {
        this.conflitoDescricao = conflitoDescricao;
    }

    public String getAutorizadoPor() {
        return autorizadoPor;
    }

    public void setAutorizadoPor(String autorizadoPor) {
        this.autorizadoPor = autorizadoPor;
    }

    public LocalDateTime getAutorizadoEm() {
        return autorizadoEm;
    }

    public void setAutorizadoEm(LocalDateTime autorizadoEm) {
        this.autorizadoEm = autorizadoEm;
    }

    public LocalDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public void setIniciadoEm(LocalDateTime iniciadoEm) {
        this.iniciadoEm = iniciadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(LocalDateTime concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getVersao() {
        return versao;
    }
}
