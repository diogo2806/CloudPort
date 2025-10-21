package br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo;

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

@Entity
@Table(name = "ordem_movimentacao",
        uniqueConstraints = @UniqueConstraint(name = "uk_ordem_visita_conteiner_tipo",
                columnNames = {"visita_trem_id", "codigo_conteiner", "tipo_movimentacao"}))
public class OrdemMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_trem_id", nullable = false)
    private VisitaTrem visitaTrem;

    @Column(name = "codigo_conteiner", nullable = false, length = 20)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false, length = 20)
    private TipoMovimentacaoOrdem tipoMovimentacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_movimentacao", nullable = false, length = 20)
    private StatusOrdemMovimentacao statusMovimentacao = StatusOrdemMovimentacao.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public OrdemMovimentacao() {
    }

    public OrdemMovimentacao(VisitaTrem visitaTrem,
                             String codigoConteiner,
                             TipoMovimentacaoOrdem tipoMovimentacao,
                             StatusOrdemMovimentacao statusMovimentacao) {
        this.visitaTrem = visitaTrem;
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

    public VisitaTrem getVisitaTrem() {
        return visitaTrem;
    }

    public void setVisitaTrem(VisitaTrem visitaTrem) {
        this.visitaTrem = visitaTrem;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        definirCodigoConteiner(codigoConteiner);
    }

    public TipoMovimentacaoOrdem getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(TipoMovimentacaoOrdem tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public StatusOrdemMovimentacao getStatusMovimentacao() {
        return statusMovimentacao;
    }

    public void setStatusMovimentacao(StatusOrdemMovimentacao statusMovimentacao) {
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
