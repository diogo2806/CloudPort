package br.com.cloudport.servicoyard.recursos.dto;

import java.time.LocalDateTime;

public class ZonaArmazenagemDTO {

    private String codigo;
    private String nome;
    private Integer capacidadeTotal;
    private Integer ocupacaoAtual;
    private double percentualOcupacao;
    private boolean bloqueada;
    private LocalDateTime atualizadoEm;
    private String observacao;

    public ZonaArmazenagemDTO() {
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getCapacidadeTotal() {
        return capacidadeTotal;
    }

    public void setCapacidadeTotal(Integer capacidadeTotal) {
        this.capacidadeTotal = capacidadeTotal;
    }

    public Integer getOcupacaoAtual() {
        return ocupacaoAtual;
    }

    public void setOcupacaoAtual(Integer ocupacaoAtual) {
        this.ocupacaoAtual = ocupacaoAtual;
    }

    public double getPercentualOcupacao() {
        return percentualOcupacao;
    }

    public void setPercentualOcupacao(double percentualOcupacao) {
        this.percentualOcupacao = percentualOcupacao;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public void setBloqueada(boolean bloqueada) {
        this.bloqueada = bloqueada;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
