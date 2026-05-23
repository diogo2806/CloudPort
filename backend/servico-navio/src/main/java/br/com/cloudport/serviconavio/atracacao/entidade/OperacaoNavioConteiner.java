package br.com.cloudport.serviconavio.atracacao.entidade;

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
@Table(name = "operacao_navio_conteiner")
public class OperacaoNavioConteiner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_id")
    private VisitaNavio visita;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 20)
    private TipoOperacaoNavioConteiner tipoOperacao;

    @Column(name = "identificacao_conteiner", nullable = false, length = 20)
    private String identificacaoConteiner;

    @Column(name = "bay")
    private Integer bay;

    @Column(name = "fileira")
    private Integer fileira;

    @Column(name = "altura")
    private Integer altura;

    @Column(name = "peso_toneladas", precision = 10, scale = 3)
    private BigDecimal pesoToneladas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusOperacaoNavioConteiner status;

    public Long getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Long identificador) {
        this.identificador = identificador;
    }

    public VisitaNavio getVisita() {
        return visita;
    }

    public void setVisita(VisitaNavio visita) {
        this.visita = visita;
    }

    public TipoOperacaoNavioConteiner getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacaoNavioConteiner tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getIdentificacaoConteiner() {
        return identificacaoConteiner;
    }

    public void setIdentificacaoConteiner(String identificacaoConteiner) {
        this.identificacaoConteiner = identificacaoConteiner;
    }

    public Integer getBay() {
        return bay;
    }

    public void setBay(Integer bay) {
        this.bay = bay;
    }

    public Integer getFileira() {
        return fileira;
    }

    public void setFileira(Integer fileira) {
        this.fileira = fileira;
    }

    public Integer getAltura() {
        return altura;
    }

    public void setAltura(Integer altura) {
        this.altura = altura;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }

    public StatusOperacaoNavioConteiner getStatus() {
        return status;
    }

    public void setStatus(StatusOperacaoNavioConteiner status) {
        this.status = status;
    }
}
