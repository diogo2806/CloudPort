package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EscalaDetalheDTO {

    private final Long id;
    private final Long navioId;
    private final String nomeNavio;
    private final String codigoImo;
    private final String viagemEntrada;
    private final String viagemSaida;
    private final FaseEscala fase;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime chegadaPrevista;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime atracacaoPrevista;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime partidaPrevista;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime chegadaEfetiva;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime atracacaoEfetiva;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime partidaEfetiva;
    private final String bercoPrevisto;
    private final String bercoAtual;
    private final String observacoes;
    private final List<OperacaoConteinerEscalaResumoDTO> listaDescarga;
    private final List<OperacaoConteinerEscalaResumoDTO> listaCarga;

    public EscalaDetalheDTO(Long id,
                            Long navioId,
                            String nomeNavio,
                            String codigoImo,
                            String viagemEntrada,
                            String viagemSaida,
                            FaseEscala fase,
                            LocalDateTime chegadaPrevista,
                            LocalDateTime atracacaoPrevista,
                            LocalDateTime partidaPrevista,
                            LocalDateTime chegadaEfetiva,
                            LocalDateTime atracacaoEfetiva,
                            LocalDateTime partidaEfetiva,
                            String bercoPrevisto,
                            String bercoAtual,
                            String observacoes,
                            List<OperacaoConteinerEscalaResumoDTO> listaDescarga,
                            List<OperacaoConteinerEscalaResumoDTO> listaCarga) {
        this.id = id;
        this.navioId = navioId;
        this.nomeNavio = nomeNavio;
        this.codigoImo = codigoImo;
        this.viagemEntrada = viagemEntrada;
        this.viagemSaida = viagemSaida;
        this.fase = fase;
        this.chegadaPrevista = chegadaPrevista;
        this.atracacaoPrevista = atracacaoPrevista;
        this.partidaPrevista = partidaPrevista;
        this.chegadaEfetiva = chegadaEfetiva;
        this.atracacaoEfetiva = atracacaoEfetiva;
        this.partidaEfetiva = partidaEfetiva;
        this.bercoPrevisto = bercoPrevisto;
        this.bercoAtual = bercoAtual;
        this.observacoes = observacoes;
        this.listaDescarga = listaDescarga;
        this.listaCarga = listaCarga;
    }

    public static EscalaDetalheDTO deEntidade(Escala escala) {
        return new EscalaDetalheDTO(
                escala.getId(),
                escala.getNavio().getIdentificador(),
                escala.getNavio().getNome(),
                escala.getNavio().getCodigoImo(),
                escala.getViagemEntrada(),
                escala.getViagemSaida(),
                escala.getFase(),
                escala.getChegadaPrevista(),
                escala.getAtracacaoPrevista(),
                escala.getPartidaPrevista(),
                escala.getChegadaEfetiva(),
                escala.getAtracacaoEfetiva(),
                escala.getPartidaEfetiva(),
                escala.getBercoPrevisto(),
                escala.getBercoAtual(),
                escala.getObservacoes(),
                mapearLista(escala.getListaDescarga()),
                mapearLista(escala.getListaCarga())
        );
    }

    private static List<OperacaoConteinerEscalaResumoDTO> mapearLista(
            List<br.com.cloudport.serviconavio.escala.entidade.OperacaoConteinerEscala> lista) {
        return Optional.ofNullable(lista).orElseGet(List::of).stream()
                .map(OperacaoConteinerEscalaResumoDTO::deEntidade)
                .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public Long getNavioId() {
        return navioId;
    }

    public String getNomeNavio() {
        return nomeNavio;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public String getViagemEntrada() {
        return viagemEntrada;
    }

    public String getViagemSaida() {
        return viagemSaida;
    }

    public FaseEscala getFase() {
        return fase;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public LocalDateTime getPartidaPrevista() {
        return partidaPrevista;
    }

    public LocalDateTime getChegadaEfetiva() {
        return chegadaEfetiva;
    }

    public LocalDateTime getAtracacaoEfetiva() {
        return atracacaoEfetiva;
    }

    public LocalDateTime getPartidaEfetiva() {
        return partidaEfetiva;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<OperacaoConteinerEscalaResumoDTO> getListaDescarga() {
        return listaDescarga;
    }

    public List<OperacaoConteinerEscalaResumoDTO> getListaCarga() {
        return listaCarga;
    }
}
