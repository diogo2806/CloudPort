package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NavioGranelDto {

    private Long id;
    private String imo;
    private String nome;
    private String classe;
    private Double lpp;
    private Double boca;
    private Double calado;
    private Double deslocamento;
    private Double gm;
    private Double tpc;
    private Double lcb;
    private Double km;
    private Double mct1cm;
    private Double caladoMaximo;
    private Double trimMaximo;
    private Double bandaMaxima;
    private Double gmMinimo;
    private Double pesoLeveToneladas;
    private Double lcgPesoLeve;
    private Double tcgPesoLeve;
    private Double vcgPesoLeve;
    private Double pesoLastroToneladas;
    private Double lcgLastro;
    private Double tcgLastro;
    private Double vcgLastro;
    private Double bmMaxPermitido;
    private Double sfMaxPermitido;
    private String versaoDadosHidrostaticos;
    private String versaoDadosEstruturais;
    private String posicoesSecoes;
    private String pesoLeveSecoes;
    private String empuxoSecoes;
    private String limitesSfSecoes;
    private String limitesBmSecoes;
    private boolean isTemplate;
    private int totalPoroes;
    private List<PoraoNavioDto> poroes = new ArrayList<>();

    public NavioGranelDto() {
    }

    public NavioGranelDto(Long id, String imo, String nome, String classe, Double lpp, Double boca,
            Double calado, Double deslocamento, Double gm, Double bmMaxPermitido, Double sfMaxPermitido,
            boolean isTemplate, int totalPoroes) {
        this.id = id;
        this.imo = imo;
        this.nome = nome;
        this.classe = classe;
        this.lpp = lpp;
        this.boca = boca;
        this.calado = calado;
        this.deslocamento = deslocamento;
        this.gm = gm;
        this.bmMaxPermitido = bmMaxPermitido;
        this.sfMaxPermitido = sfMaxPermitido;
        this.isTemplate = isTemplate;
        this.totalPoroes = totalPoroes;
    }
}
