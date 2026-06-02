package br.com.cloudport.visibilidade.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "capacidade_yard")
public class CapacidadeYard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zona", unique = true)
    private String zona;

    @Column(name = "capacidade_total")
    private Integer capacidadeTotal;

    @Column(name = "ocupacao_atual")
    private Integer ocupacaoAtual;

    @Column(name = "percentual_ocupacao")
    private Double percentualOcupacao;

    @Column(name = "equipamentos_disponiveis")
    private Integer equipamentosDisponiveis;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public Integer getCapacidadeTotal() { return capacidadeTotal; }
    public void setCapacidadeTotal(Integer capacidadeTotal) { this.capacidadeTotal = capacidadeTotal; }

    public Integer getOcupacaoAtual() { return ocupacaoAtual; }
    public void setOcupacaoAtual(Integer ocupacaoAtual) { this.ocupacaoAtual = ocupacaoAtual; }

    public Double getPercentualOcupacao() { return percentualOcupacao; }
    public void setPercentualOcupacao(Double percentualOcupacao) { this.percentualOcupacao = percentualOcupacao; }

    public Integer getEquipamentosDisponiveis() { return equipamentosDisponiveis; }
    public void setEquipamentosDisponiveis(Integer equipamentosDisponiveis) { this.equipamentosDisponiveis = equipamentosDisponiveis; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
