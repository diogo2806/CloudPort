package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.TipoVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.util.HtmlUtils;

public class VisitaTremRespostaDto {

    private final Long id;
    private final Long versao;
    private final String identificadorTrem;
    private final String operadoraFerroviaria;
    private final TipoVisitaTrem tipoVisita;
    private final LocalDateTime horaChegadaPrevista;
    private final LocalDateTime horaPartidaPrevista;
    private final StatusVisitaTrem statusVisita;
    private final String posicaoFerroviariaAtual;
    private final List<OperacaoConteinerVisitaRespostaDto> listaDescarga;
    private final List<OperacaoConteinerVisitaRespostaDto> listaCarga;
    private final List<VagaoVisitaRespostaDto> listaVagoes;

    public VisitaTremRespostaDto(Long id,
                                  Long versao,
                                  String identificadorTrem,
                                  String operadoraFerroviaria,
                                  TipoVisitaTrem tipoVisita,
                                  LocalDateTime horaChegadaPrevista,
                                  LocalDateTime horaPartidaPrevista,
                                  StatusVisitaTrem statusVisita,
                                  String posicaoFerroviariaAtual,
                                  List<OperacaoConteinerVisitaRespostaDto> listaDescarga,
                                  List<OperacaoConteinerVisitaRespostaDto> listaCarga,
                                  List<VagaoVisitaRespostaDto> listaVagoes) {
        this.id = id;
        this.versao = versao;
        this.identificadorTrem = identificadorTrem;
        this.operadoraFerroviaria = operadoraFerroviaria;
        this.tipoVisita = tipoVisita;
        this.horaChegadaPrevista = horaChegadaPrevista;
        this.horaPartidaPrevista = horaPartidaPrevista;
        this.statusVisita = statusVisita;
        this.posicaoFerroviariaAtual = posicaoFerroviariaAtual;
        this.listaDescarga = listaDescarga;
        this.listaCarga = listaCarga;
        this.listaVagoes = listaVagoes;
    }

    public static VisitaTremRespostaDto deEntidade(VisitaTrem entidade) {
        return new VisitaTremRespostaDto(
                entidade.getId(),
                entidade.getVersao(),
                HtmlUtils.htmlEscape(entidade.getIdentificadorTrem()),
                HtmlUtils.htmlEscape(entidade.getOperadoraFerroviaria()),
                entidade.getTipoVisita(),
                entidade.getHoraChegadaPrevista(),
                entidade.getHoraPartidaPrevista(),
                entidade.getStatusVisita(),
                escapar(entidade.getPosicaoFerroviariaAtual()),
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
                entidade.getVersao(),
                HtmlUtils.htmlEscape(entidade.getIdentificadorTrem()),
                HtmlUtils.htmlEscape(entidade.getOperadoraFerroviaria()),
                entidade.getTipoVisita(),
                entidade.getHoraChegadaPrevista(),
                entidade.getHoraPartidaPrevista(),
                entidade.getStatusVisita(),
                escapar(entidade.getPosicaoFerroviariaAtual()),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private static String escapar(String valor) {
        return valor == null ? null : HtmlUtils.htmlEscape(valor);
    }

    public Long getId() {
        return id;
    }

    public Long getVersao() {
        return versao;
    }

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public String getOperadoraFerroviaria() {
        return operadoraFerroviaria;
    }

    public TipoVisitaTrem getTipoVisita() {
        return tipoVisita;
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

    public String getPosicaoFerroviariaAtual() {
        return posicaoFerroviariaAtual;
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
