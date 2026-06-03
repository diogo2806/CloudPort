package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class PressaoTanktopDto {

    private Long setorId;
    private String nomeSetor;
    private double pressaoCalculadaTM2;
    private double capacidadeNominalTM2;
    private double percentualOcupacao;
    private boolean excedido;
    private List<ViolacaoEstivaDto> violacoes;

    public PressaoTanktopDto() {
    }

    public PressaoTanktopDto(Long setorId, String nomeSetor, double pressaoCalculadaTM2,
            double capacidadeNominalTM2, double percentualOcupacao, boolean excedido,
            List<ViolacaoEstivaDto> violacoes) {
        this.setorId = setorId;
        this.nomeSetor = nomeSetor;
        this.pressaoCalculadaTM2 = pressaoCalculadaTM2;
        this.capacidadeNominalTM2 = capacidadeNominalTM2;
        this.percentualOcupacao = percentualOcupacao;
        this.excedido = excedido;
        this.violacoes = violacoes;
    }

    public static PressaoTanktopDto ok(Long id, String nome, double pressao, double cap) {
        double percentual = (cap > 0) ? (pressao / cap) * 100.0 : 0.0;
        return new PressaoTanktopDto(id, nome, pressao, cap, percentual, false, new ArrayList<>());
    }

    public static PressaoTanktopDto excedido(Long id, String nome, double pressao, double cap, String msg) {
        double percentual = (cap > 0) ? (pressao / cap) * 100.0 : 0.0;
        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        violacoes.add(new ViolacaoEstivaDto("PRESSAO_TANKTOP", msg, id, "PERIGO"));
        return new PressaoTanktopDto(id, nome, pressao, cap, percentual, true, violacoes);
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
    }

    public String getNomeSetor() {
        return nomeSetor;
    }

    public void setNomeSetor(String nomeSetor) {
        this.nomeSetor = nomeSetor;
    }

    public double getPressaoCalculadaTM2() {
        return pressaoCalculadaTM2;
    }

    public void setPressaoCalculadaTM2(double pressaoCalculadaTM2) {
        this.pressaoCalculadaTM2 = pressaoCalculadaTM2;
    }

    public double getCapacidadeNominalTM2() {
        return capacidadeNominalTM2;
    }

    public void setCapacidadeNominalTM2(double capacidadeNominalTM2) {
        this.capacidadeNominalTM2 = capacidadeNominalTM2;
    }

    public double getPercentualOcupacao() {
        return percentualOcupacao;
    }

    public void setPercentualOcupacao(double percentualOcupacao) {
        this.percentualOcupacao = percentualOcupacao;
    }

    public boolean isExcedido() {
        return excedido;
    }

    public void setExcedido(boolean excedido) {
        this.excedido = excedido;
    }

    public List<ViolacaoEstivaDto> getViolacoes() {
        return violacoes;
    }

    public void setViolacoes(List<ViolacaoEstivaDto> violacoes) {
        this.violacoes = violacoes;
    }
}
