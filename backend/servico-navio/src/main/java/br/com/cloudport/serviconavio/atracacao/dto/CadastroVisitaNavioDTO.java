package br.com.cloudport.serviconavio.atracacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CadastroVisitaNavioDTO {

    @NotNull(message = "Informe o navio da visita.")
    private Long navioId;

    @NotBlank(message = "Informe o número da viagem.")
    @Size(max = 40, message = "O número da viagem deve ter no máximo 40 caracteres.")
    private String numeroViagem;

    @NotNull(message = "Informe a data prevista de atracação.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime atracacaoPrevista;

    @NotNull(message = "Informe a data prevista de desatracação.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime desatracacaoPrevista;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;

    public Long getNavioId() {
        return navioId;
    }

    public void setNavioId(Long navioId) {
        this.navioId = navioId;
    }

    public String getNumeroViagem() {
        return numeroViagem;
    }

    public void setNumeroViagem(String numeroViagem) {
        this.numeroViagem = numeroViagem;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public void setAtracacaoPrevista(LocalDateTime atracacaoPrevista) {
        this.atracacaoPrevista = atracacaoPrevista;
    }

    public LocalDateTime getDesatracacaoPrevista() {
        return desatracacaoPrevista;
    }

    public void setDesatracacaoPrevista(LocalDateTime desatracacaoPrevista) {
        this.desatracacaoPrevista = desatracacaoPrevista;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
