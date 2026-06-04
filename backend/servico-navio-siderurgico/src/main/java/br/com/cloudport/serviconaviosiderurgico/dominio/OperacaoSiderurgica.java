package br.com.cloudport.serviconaviosiderurgico.dominio;

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

@Entity
@Table(name = "operacao_siderurgica")
public class OperacaoSiderurgica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "navio_id")
    private NavioSiderurgico navio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 20)
    private TipoOperacaoSiderurgica tipoOperacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOperacaoSiderurgica status = StatusOperacaoSiderurgica.PLANEJADA;

    @Column(length = 30)
    private String berco;

    @Column(length = 40)
    private String viagem;

    private LocalDateTime eta;

    @Column(name = "inicio_operacao")
    private LocalDateTime inicioOperacao;

    @Column(name = "fim_operacao")
    private LocalDateTime fimOperacao;

    @Column(length = 80)
    private String origem;

    @Column(length = 80)
    private String destino;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public NavioSiderurgico getNavio() { return navio; }
    public void setNavio(NavioSiderurgico navio) { this.navio = navio; }
    public TipoOperacaoSiderurgica getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoSiderurgica tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public StatusOperacaoSiderurgica getStatus() { return status; }
    public void setStatus(StatusOperacaoSiderurgica status) { this.status = status; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public String getViagem() { return viagem; }
    public void setViagem(String viagem) { this.viagem = viagem; }
    public LocalDateTime getEta() { return eta; }
    public void setEta(LocalDateTime eta) { this.eta = eta; }
    public LocalDateTime getInicioOperacao() { return inicioOperacao; }
    public void setInicioOperacao(LocalDateTime inicioOperacao) { this.inicioOperacao = inicioOperacao; }
    public LocalDateTime getFimOperacao() { return fimOperacao; }
    public void setFimOperacao(LocalDateTime fimOperacao) { this.fimOperacao = fimOperacao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
