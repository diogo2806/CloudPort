package br.com.cloudport.servicorail.ferrovia.locomotiva.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.AssertTrue;

public class LiberacaoEmbarqueLocomotivaDto {

    @AssertTrue(message = "O freio de estacionamento deve estar aplicado.")
    private boolean freioEstacionamentoAplicado;

    @AssertTrue(message = "As baterias devem estar isoladas.")
    private boolean bateriasIsoladas;

    @AssertTrue(message = "O sistema de combustível deve estar protegido.")
    private boolean combustivelProtegido;

    @AssertTrue(message = "Os calços devem estar instalados.")
    private boolean calcosInstalados;

    @AssertTrue(message = "O plano de amarração deve estar aprovado.")
    private boolean planoAmarracaoAprovado;

    private LocalDateTime liberadaEm;

    public boolean isFreioEstacionamentoAplicado() { return freioEstacionamentoAplicado; }
    public void setFreioEstacionamentoAplicado(boolean freioEstacionamentoAplicado) { this.freioEstacionamentoAplicado = freioEstacionamentoAplicado; }
    public boolean isBateriasIsoladas() { return bateriasIsoladas; }
    public void setBateriasIsoladas(boolean bateriasIsoladas) { this.bateriasIsoladas = bateriasIsoladas; }
    public boolean isCombustivelProtegido() { return combustivelProtegido; }
    public void setCombustivelProtegido(boolean combustivelProtegido) { this.combustivelProtegido = combustivelProtegido; }
    public boolean isCalcosInstalados() { return calcosInstalados; }
    public void setCalcosInstalados(boolean calcosInstalados) { this.calcosInstalados = calcosInstalados; }
    public boolean isPlanoAmarracaoAprovado() { return planoAmarracaoAprovado; }
    public void setPlanoAmarracaoAprovado(boolean planoAmarracaoAprovado) { this.planoAmarracaoAprovado = planoAmarracaoAprovado; }
    public LocalDateTime getLiberadaEm() { return liberadaEm; }
    public void setLiberadaEm(LocalDateTime liberadaEm) { this.liberadaEm = liberadaEm; }
}
