package br.com.cloudport.servicoyard.estivagembulk.modelo;

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
@Table(name = "porto_viagem")
public class PortoViagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id", nullable = false)
    private PlanoEstivaBulk plano;

    @Column(name = "codigo_porto", length = 10, nullable = false)
    private String codigoPorto;

    @Column(name = "nome_porto", length = 100)
    private String nomePorto;

    @Column(name = "sequencia", nullable = false)
    private Integer sequencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", length = 20)
    private TipoOperacaoPorto tipoOperacao;

    @Column(name = "calado_maximo_m")
    private Double caladoMaximoM;

    @Column(name = "restricao_aire_m")
    private Double restricaoAireM;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlanoEstivaBulk getPlano() { return plano; }
    public void setPlano(PlanoEstivaBulk plano) { this.plano = plano; }

    public String getCodigoPorto() { return codigoPorto; }
    public void setCodigoPorto(String codigoPorto) { this.codigoPorto = codigoPorto; }

    public String getNomePorto() { return nomePorto; }
    public void setNomePorto(String nomePorto) { this.nomePorto = nomePorto; }

    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }

    public TipoOperacaoPorto getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoPorto tipoOperacao) { this.tipoOperacao = tipoOperacao; }

    public Double getCaladoMaximoM() { return caladoMaximoM; }
    public void setCaladoMaximoM(Double caladoMaximoM) { this.caladoMaximoM = caladoMaximoM; }

    public Double getRestricaoAireM() { return restricaoAireM; }
    public void setRestricaoAireM(Double restricaoAireM) { this.restricaoAireM = restricaoAireM; }
}
