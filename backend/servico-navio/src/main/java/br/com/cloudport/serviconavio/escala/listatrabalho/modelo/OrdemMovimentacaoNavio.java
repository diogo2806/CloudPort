package br.com.cloudport.serviconavio.escala.listatrabalho.modelo;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
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

@Entity
@Table(name = "ordem_movimentacao_navio",
        uniqueConstraints = @UniqueConstraint(name = "uk_ordem_navio_escala_conteiner_tipo",
                columnNames = {"escala_id", "codigo_conteiner", "tipo_movimentacao"}))
public class OrdemMovimentacaoNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false)
    private Escala escala;

    @Column(name = "codigo_conteiner", nullable = false, length = 20)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false, length = 20)
    private TipoMovimentacaoOrdemNavio tipoMovimentacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_movimentacao", nullable = false, length = 20)
    private StatusOrdemMovimentacaoNavio statusMovimentacao = StatusOrdemMovimentacaoNavio.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public OrdemMovimentacaoNavio() {
    }

    public OrdemMovimentacaoNavio(Escala escala,
                                  String codigoConteiner,
                                  TipoMovimentacaoOrdemNavio tipoMovimentacao,
                                  StatusOrdemMovimentacaoNavio statusMovimentacao) {
        this.escala = escala;
        definirCodigoConteiner(codigoConteiner);
        this.tipoMovimentacao = tipoMovimentacao;
        this.statusMovimentacao = statusMovimentacao;
    }

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Escala getEscala() {
        return escala;
    }

    public void setEscala(Escala escala) {
        this.escala = escala;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        definirCodigoConteiner(codigoConteiner);
    }

    public TipoMovimentacaoOrdemNavio getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(TipoMovimentacaoOrdemNavio tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public StatusOrdemMovimentacaoNavio getStatusMovimentacao() {
        return statusMovimentacao;
    }

    public void setStatusMovimentacao(StatusOrdemMovimentacaoNavio statusMovimentacao) {
        this.statusMovimentacao = statusMovimentacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    private void definirCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner != null ? codigoConteiner.trim().toUpperCase() : null;
    }
}
