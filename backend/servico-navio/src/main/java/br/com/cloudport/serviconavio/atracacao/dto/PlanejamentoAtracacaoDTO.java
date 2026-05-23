package br.com.cloudport.serviconavio.atracacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

public class PlanejamentoAtracacaoDTO {

    @NotNull(message = "Informe o berço para a atracação.")
    private Long bercoId;

    @NotNull(message = "Informe a data prevista de atracação.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime atracacaoPrevista;

    @NotNull(message = "Informe a data prevista de desatracação.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime desatracacaoPrevista;

    public Long getBercoId() {
        return bercoId;
    }

    public void setBercoId(Long bercoId) {
        this.bercoId = bercoId;
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
}
