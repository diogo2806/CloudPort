package br.com.cloudport.servicoyard.ferrovia.dto;

import br.com.cloudport.servicoyard.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicoyard.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import org.springframework.web.util.HtmlUtils;

public class VisitaTremRespostaDto {

    private final Long id;
    private final String identificadorTrem;
    private final String operadoraFerroviaria;
    private final LocalDateTime horaChegadaPrevista;
    private final LocalDateTime horaPartidaPrevista;
    private final StatusVisitaTrem statusVisita;

    public VisitaTremRespostaDto(Long id,
                                 String identificadorTrem,
                                 String operadoraFerroviaria,
                                 LocalDateTime horaChegadaPrevista,
                                 LocalDateTime horaPartidaPrevista,
                                 StatusVisitaTrem statusVisita) {
        this.id = id;
        this.identificadorTrem = identificadorTrem;
        this.operadoraFerroviaria = operadoraFerroviaria;
        this.horaChegadaPrevista = horaChegadaPrevista;
        this.horaPartidaPrevista = horaPartidaPrevista;
        this.statusVisita = statusVisita;
    }

    public static VisitaTremRespostaDto deEntidade(VisitaTrem entidade) {
        return new VisitaTremRespostaDto(
                entidade.getId(),
                HtmlUtils.htmlEscape(entidade.getIdentificadorTrem()),
                HtmlUtils.htmlEscape(entidade.getOperadoraFerroviaria()),
                entidade.getHoraChegadaPrevista(),
                entidade.getHoraPartidaPrevista(),
                entidade.getStatusVisita()
        );
    }

    public Long getId() {
        return id;
    }

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public String getOperadoraFerroviaria() {
        return operadoraFerroviaria;
    }

    public LocalDateTime getHoraChegadaPrevista() {
        return horaChegadaPrevista;
    }

    public LocalDateTime getHoraPartidaPrevista() {
        return horaPartidaPrevista;
    }

    public StatusVisitaTrem getStatusVisita() {
        return statusVisita;
    }
}
