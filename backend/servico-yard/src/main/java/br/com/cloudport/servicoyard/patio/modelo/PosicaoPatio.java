package br.com.cloudport.servicoyard.patio.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "posicao_patio", uniqueConstraints = {
        @UniqueConstraint(name = "uk_posicao_unica", columnNames = {"linha", "coluna", "camada_operacional"})
})
public class PosicaoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "linha", nullable = false)
    private Integer linha;

    @Column(name = "coluna", nullable = false)
    private Integer coluna;

    @Column(name = "camada_operacional", nullable = false, length = 40)
    private String camadaOperacional;

    public PosicaoPatio() {
    }

    public PosicaoPatio(Long id, Integer linha, Integer coluna, String camadaOperacional) {
        this.id = id;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public void setCamadaOperacional(String camadaOperacional) {
        this.camadaOperacional = camadaOperacional;
    }
}
