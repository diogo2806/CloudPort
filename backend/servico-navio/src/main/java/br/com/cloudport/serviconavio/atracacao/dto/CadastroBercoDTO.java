package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusBerco;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CadastroBercoDTO {

    @NotBlank(message = "Informe o nome do berço.")
    @Size(max = 60, message = "O nome do berço deve ter no máximo 60 caracteres.")
    private String nome;

    @NotNull(message = "Informe o comprimento do berço.")
    @DecimalMin(value = "0.0", inclusive = false, message = "O comprimento deve ser maior que zero.")
    private BigDecimal comprimentoMetros;

    @NotNull(message = "Informe o calado máximo do berço.")
    @DecimalMin(value = "0.0", inclusive = false, message = "O calado máximo deve ser maior que zero.")
    private BigDecimal caladoMaximoMetros;

    private StatusBerco status;

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
