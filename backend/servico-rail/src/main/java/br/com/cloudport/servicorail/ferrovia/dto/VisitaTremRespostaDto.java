package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.util.HtmlUtils;

public class VisitaTremRespostaDto {

    private final Long id;
    private final String identificadorTrem;
    private final String operadoraFerroviaria;
    private final LocalDateTime horaChegadaPrevista;
    private final LocalDateTime horaPartidaPrevista;
    private final StatusVisitaTrem statusVisita;
    private final List<OperacaoConteinerVisitaRespostaDto> listaDescarga;
    private final List<OperacaoConteinerVisitaRespostaDto> listaCarga;
    private final List<VagaoVisitaRespostaDto> listaVagoes;

    public VisitaTremRespostaDto(Long id,
                                 String identificadorTrem,
                                 String operadoraFerroviaria,
                                 LocalDateTime horaChegadaPrevista,
                                 LocalDateTime horaPartidaPrevista,
                                 StatusVisitaTrem statusVisita,
                                 List<OperacaoConteinerVisitaRespostaDto> listaDescarga,
                                 List<OperacaoConteinerVisitaRespostaDto> listaCarga,
                                 List<VagaoVisitaRespostaDto> listaVagoes) {
        this.id = id;
        this.identificadorTrem = identificadorTrem;
        this.operadoraFerroviaria = operadoraFerroviaria;
        this.horaChegadaPrevista = horaChegadaPrevista;
        this.horaPartidaPrevista = horaPartidaPrevista;
        this.statusVisita = statusVisita;
        this.listaDescarga = listaDescarga;
        this.listaCarga = listaCarga;
        this.listaVagoes = listaVagoes;
    }

    public static VisitaTremRespostaDto deEntidade(VisitaTrem entidade) {
        return new VisitaTremRespostaDto(
                entidade.getId(),
                HtmlUtils.htmlEscape(entidade.getIdentificadorTrem()),
                HtmlUtils.htmlEscape(entidade.getOperadoraFerroviaria()),
                entidade.getHoraChegadaPrevista(),
                entidade.getHoraPartidaPrevista(),
                entidade.getStatusVisita(),
                entidade.getListaDescarga()
                        .stream()
                        .map(OperacaoConteinerVisitaRespostaDto::deEmbeddable)
                        .collect(Collectors.toList()),
                entidade.getListaCarga()
                        .stream()
                        .map(OperacaoConteinerVisitaRespostaDto::deEmbeddable)
                        .collect(Collectors.toList()),
                entidade.getListaVagoes()
                        .stream()
                        .map(VagaoVisitaRespostaDto::deEmbeddable)
                        .collect(Collectors.toList())
        );
    }

    public static VisitaTremRespostaDto deEntidadeSemListas(VisitaTrem entidade) {
        return new VisitaTremRespostaDto(
                entidade.getId(),
                HtmlUtils.htmlEscape(entidade.getIdentificadorTrem()),
                HtmlUtils.htmlEscape(entidade.getOperadoraFerroviaria()),
                entidade.getHoraChegadaPrevista(),
                entidade.getHoraPartidaPrevista(),
                entidade.getStatusVisita(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
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

    public List<OperacaoConteinerVisitaRespostaDto> getListaDescarga() {
        return listaDescarga;
    }

    public List<OperacaoConteinerVisitaRespostaDto> getListaCarga() {
        return listaCarga;
    }

    public List<VagaoVisitaRespostaDto> getListaVagoes() {
        return listaVagoes;
    }
}
