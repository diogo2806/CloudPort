package br.com.cloudport.serviconavio.escala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class AtualizacaoEscalaDTO {

    @Size(max = 20, message = "A viagem de entrada deve ter no máximo 20 caracteres.")
    private String viagemEntrada;

    @Size(max = 20, message = "A viagem de saída deve ter no máximo 20 caracteres.")
    private String viagemSaida;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime chegadaPrevista;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime atracacaoPrevista;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime partidaPrevista;

    @Size(max = 20, message = "O berço previsto deve ter no máximo 20 caracteres.")
    private String bercoPrevisto;

    @Size(max = 20, message = "O berço atual deve ter no máximo 20 caracteres.")
    private String bercoAtual;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;

    public AtualizacaoEscalaDTO() {
    }

    public String getViagemEntrada() {
        return viagemEntrada;
    }

    public void setViagemEntrada(String viagemEntrada) {
        this.viagemEntrada = viagemEntrada;
    }

    public String getViagemSaida() {
        return viagemSaida;
    }

    public void setViagemSaida(String viagemSaida) {
        this.viagemSaida = viagemSaida;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public void setChegadaPrevista(LocalDateTime chegadaPrevista) {
        this.chegadaPrevista = chegadaPrevista;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public void setAtracacaoPrevista(LocalDateTime atracacaoPrevista) {
        this.atracacaoPrevista = atracacaoPrevista;
    }

    public LocalDateTime getPartidaPrevista() {
        return partidaPrevista;
    }

    public void setPartidaPrevista(LocalDateTime partidaPrevista) {
        this.partidaPrevista = partidaPrevista;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public void setBercoPrevisto(String bercoPrevisto) {
        this.bercoPrevisto = bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public void setBercoAtual(String bercoAtual) {
        this.bercoAtual = bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
