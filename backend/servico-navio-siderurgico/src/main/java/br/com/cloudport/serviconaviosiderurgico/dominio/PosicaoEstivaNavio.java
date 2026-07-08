package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.math.BigDecimal;
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
@Table(name = "posicao_estiva_navio")
public class PosicaoEstivaNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_estiva_id", nullable = false)
    private PlanoEstivaNavio planoEstiva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_operacao_id", nullable = false)
    private ItemOperacaoNavio itemOperacao;

    @Column(nullable = false)
    private Integer porao;

    @Column(nullable = false)
    private Integer camada;

    @Column(nullable = false)
    private Integer coluna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BordoEstivaNavio bordo;

    @Column(nullable = false)
    private Integer sequencia;

    @Column(name = "peso_toneladas", nullable = false, precision = 14, scale = 3)
    private BigDecimal pesoToneladas;

    @Column(nullable = false, length = 30)
    private String status = "PLANEJADO";

    public Long getId() { return id; }
    public PlanoEstivaNavio getPlanoEstiva() { return planoEstiva; }
    public void setPlanoEstiva(PlanoEstivaNavio planoEstiva) { this.planoEstiva = planoEstiva; }
    public ItemOperacaoNavio getItemOperacao() { return itemOperacao; }
    public void setItemOperacao(ItemOperacaoNavio itemOperacao) { this.itemOperacao = itemOperacao; }
    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public Integer getCamada() { return camada; }
    public void setCamada(Integer camada) { this.camada = camada; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
    public BordoEstivaNavio getBordo() { return bordo; }
    public void setBordo(BordoEstivaNavio bordo) { this.bordo = bordo; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }
    public BigDecimal getPesoToneladas() { return pesoToneladas; }
    public void setPesoToneladas(BigDecimal pesoToneladas) { this.pesoToneladas = pesoToneladas; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
