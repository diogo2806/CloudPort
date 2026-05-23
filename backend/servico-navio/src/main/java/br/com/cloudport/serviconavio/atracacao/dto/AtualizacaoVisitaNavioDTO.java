package br.com.cloudport.serviconavio.atracacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.Size;

public class AtualizacaoVisitaNavioDTO {

    @Size(max = 40, message = "O número da viagem deve ter no máximo 40 caracteres.")
    private String numeroViagem;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime atracacaoPrevista;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime desatracacaoPrevista;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;

    private Long servicoId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime chegadaPrevista;

    public Long getServicoId() {
        return servicoId;
    }

    public void setServicoId(Long servicoId) {
        this.servicoId = servicoId;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public void setChegadaPrevista(LocalDateTime chegadaPrevista) {
        this.chegadaPrevista = chegadaPrevista;
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
