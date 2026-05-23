package br.com.cloudport.serviconavio.atracacao.entidade;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "berco")
public class Berco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @Column(name = "nome", nullable = false, unique = true, length = 60)
    private String nome;

    @Column(name = "comprimento_metros", nullable = false, precision = 8, scale = 2)
    private BigDecimal comprimentoMetros;

    @Column(name = "calado_maximo_metros", nullable = false, precision = 6, scale = 2)
    private BigDecimal caladoMaximoMetros;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusBerco status;

    public Long getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Long identificador) {
        this.identificador = identificador;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getComprimentoMetros() {
        return comprimentoMetros;
    }

    public void setComprimentoMetros(BigDecimal comprimentoMetros) {
        this.comprimentoMetros = comprimentoMetros;
    }

    public BigDecimal getCaladoMaximoMetros() {
        return caladoMaximoMetros;
    }

    public void setCaladoMaximoMetros(BigDecimal caladoMaximoMetros) {
        this.caladoMaximoMetros = caladoMaximoMetros;
    }

    public StatusBerco getStatus() {
        return status;
    }

    public void setStatus(StatusBerco status) {
        this.status = status;
    }
}
