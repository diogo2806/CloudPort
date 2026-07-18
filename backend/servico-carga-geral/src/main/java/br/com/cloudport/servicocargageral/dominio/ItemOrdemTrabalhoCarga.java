package br.com.cloudport.servicocargageral.dominio;

import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "item_ordem_trabalho_carga")
public class ItemOrdemTrabalhoCarga {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ordem_id", nullable = false) private OrdemTrabalhoCarga ordem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lote_id", nullable = false) private LoteCarga lote;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal quantidade;
    @Column(length = 500) private String observacao;
    public UUID getId() { return id; }
    public OrdemTrabalhoCarga getOrdem() { return ordem; }
    public void setOrdem(OrdemTrabalhoCarga ordem) { this.ordem = ordem; }
    public LoteCarga getLote() { return lote; }
    public void setLote(LoteCarga lote) { this.lote = lote; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { if (quantidade == null || quantidade.signum() <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva."); this.quantidade = quantidade; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao == null ? null : observacao.trim(); }
}
