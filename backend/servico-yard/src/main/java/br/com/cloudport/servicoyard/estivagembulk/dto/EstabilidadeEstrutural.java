package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstabilidadeEstrutural {

    private double bmMaxKnm;
    private double sfMaxKn;
    private double trimMetros;
    private double listGraus;
    private double gmMetros;
    private double caladoSaidaMetros;
    private double pesoTotalToneladas;
    private boolean hogging;
    private boolean sagging;
    private boolean operacional;
    private boolean aprovado;
    private String versaoDadosHidrostaticos;
    private String versaoDadosEstruturais;
    private String memoriaCalculo;
    private List<ViolacaoEstivaDto> violacoes;

    public EstabilidadeEstrutural() {
    }

    public EstabilidadeEstrutural(double bmMaxKnm, double sfMaxKn, double trimMetros,
            double caladoSaidaMetros, double pesoTotalToneladas, boolean hogging, boolean sagging,
            boolean aprovado, List<ViolacaoEstivaDto> violacoes) {
        this.bmMaxKnm = bmMaxKnm;
        this.sfMaxKn = sfMaxKn;
        this.trimMetros = trimMetros;
        this.caladoSaidaMetros = caladoSaidaMetros;
        this.pesoTotalToneladas = pesoTotalToneladas;
        this.hogging = hogging;
        this.sagging = sagging;
        this.operacional = aprovado;
        this.aprovado = aprovado;
        this.violacoes = violacoes;
    }

    public static EstabilidadeEstrutural vazia() {
        EstabilidadeEstrutural dto = new EstabilidadeEstrutural();
        dto.operacional = false;
        dto.aprovado = false;
        dto.violacoes = new ArrayList<>();
        return dto;
    }
}
