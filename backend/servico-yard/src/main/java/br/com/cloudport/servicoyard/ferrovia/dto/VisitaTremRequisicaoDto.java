package br.com.cloudport.servicoyard.ferrovia.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.ferrovia.modelo.StatusVisitaTrem;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class VisitaTremRequisicaoDto {

    @NotBlank
    @Size(max = 40)
    private String identificadorTrem;

    @NotBlank
    @Size(max = 80)
    private String operadoraFerroviaria;

    @NotNull
    private LocalDateTime horaChegadaPrevista;

    @NotNull
    private LocalDateTime horaPartidaPrevista;

    @NotNull
    private StatusVisitaTrem statusVisita;

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public void setIdentificadorTrem(String identificadorTrem) {
        this.identificadorTrem = ValidacaoEntradaUtil.limparTexto(identificadorTrem);
    }

    public String getOperadoraFerroviaria() {
        return operadoraFerroviaria;
    }

    public void setOperadoraFerroviaria(String operadoraFerroviaria) {
        this.operadoraFerroviaria = ValidacaoEntradaUtil.limparTexto(operadoraFerroviaria);
    }

    public LocalDateTime getHoraChegadaPrevista() {
        return horaChegadaPrevista;
    }

    public void setHoraChegadaPrevista(LocalDateTime horaChegadaPrevista) {
        this.horaChegadaPrevista = horaChegadaPrevista;
    }

    public LocalDateTime getHoraPartidaPrevista() {
        return horaPartidaPrevista;
    }

    public void setHoraPartidaPrevista(LocalDateTime horaPartidaPrevista) {
        this.horaPartidaPrevista = horaPartidaPrevista;
    }

    public StatusVisitaTrem getStatusVisita() {
        return statusVisita;
    }

    public void setStatusVisita(StatusVisitaTrem statusVisita) {
        this.statusVisita = statusVisita;
    }
}
