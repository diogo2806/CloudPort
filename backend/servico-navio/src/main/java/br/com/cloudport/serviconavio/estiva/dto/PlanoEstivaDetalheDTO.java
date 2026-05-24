package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.StatusPlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.TiersEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.TipoOperacaoEstiva;
import java.util.List;
import java.util.stream.Collectors;

public class PlanoEstivaDetalheDTO {

    private final Long id;
    private final Long escalaId;
    private final String nomeNavio;
    private final String viagemEntrada;
    private final StatusPlanoEstiva status;
    private final int baias;
    private final int fileiras;
    private final int camadasPorao;
    private final int camadasConves;
    private final List<Integer> tiersPorao;
    private final List<Integer> tiersConves;
    private final int capacidadeCelulas;
    private final int embarquePlanejado;
    private final int embarqueExecutado;
    private final int embarquePendente;
    private final int descargaPlanejada;
    private final int descargaExecutada;
    private final int descargaPendente;
    private final List<AtribuicaoEstivaDTO> atribuicoes;
    private final List<TernoDTO> ternos;

    public PlanoEstivaDetalheDTO(Long id,
                                 Long escalaId,
                                 String nomeNavio,
                                 String viagemEntrada,
                                 StatusPlanoEstiva status,
                                 int baias,
                                 int fileiras,
                                 int camadasPorao,
                                 int camadasConves,
                                 List<Integer> tiersPorao,
                                 List<Integer> tiersConves,
                                 int capacidadeCelulas,
                                 int embarquePlanejado,
                                 int embarqueExecutado,
                                 int embarquePendente,
                                 int descargaPlanejada,
                                 int descargaExecutada,
                                 int descargaPendente,
                                 List<AtribuicaoEstivaDTO> atribuicoes,
                                 List<TernoDTO> ternos) {
        this.id = id;
        this.escalaId = escalaId;
        this.nomeNavio = nomeNavio;
        this.viagemEntrada = viagemEntrada;
        this.status = status;
        this.baias = baias;
        this.fileiras = fileiras;
        this.camadasPorao = camadasPorao;
        this.camadasConves = camadasConves;
        this.tiersPorao = tiersPorao;
        this.tiersConves = tiersConves;
        this.capacidadeCelulas = capacidadeCelulas;
        this.embarquePlanejado = embarquePlanejado;
        this.embarqueExecutado = embarqueExecutado;
        this.embarquePendente = embarquePendente;
        this.descargaPlanejada = descargaPlanejada;
        this.descargaExecutada = descargaExecutada;
        this.descargaPendente = descargaPendente;
        this.atribuicoes = atribuicoes;
        this.ternos = ternos;
    }

    public static PlanoEstivaDetalheDTO deEntidade(PlanoEstiva plano) {
        List<AtribuicaoEstivaDTO> atribuicoes = plano.getAtribuicoes().stream()
                .map(AtribuicaoEstivaDTO::deEntidade)
                .collect(Collectors.toList());

        int embarquePlanejado = (int) atribuicoes.stream()
                .filter(a -> a.getTipoOperacao() == TipoOperacaoEstiva.EMBARQUE).count();
        int embarqueExecutado = (int) atribuicoes.stream()
                .filter(a -> a.getTipoOperacao() == TipoOperacaoEstiva.EMBARQUE && a.isEmbarcado()).count();
        int descargaPlanejada = (int) atribuicoes.stream()
                .filter(a -> a.getTipoOperacao() == TipoOperacaoEstiva.DESCARGA).count();
        int descargaExecutada = (int) atribuicoes.stream()
                .filter(a -> a.getTipoOperacao() == TipoOperacaoEstiva.DESCARGA && a.isEmbarcado()).count();

        List<TernoDTO> ternos = plano.getTernos().stream()
                .map(TernoDTO::deEntidade)
                .collect(Collectors.toList());

        Escala escala = plano.getEscala();
        return new PlanoEstivaDetalheDTO(
                plano.getId(),
                escala.getId(),
                escala.getNavio().getNome(),
                escala.getViagemEntrada(),
                plano.getStatus(),
                plano.getBaias(),
                plano.getFileiras(),
                plano.getCamadasPorao(),
                plano.getCamadasConves(),
                TiersEstiva.tiersPorao(plano.getCamadasPorao()),
                TiersEstiva.tiersConves(plano.getCamadasConves()),
                plano.capacidadeCelulas(),
                embarquePlanejado,
                embarqueExecutado,
                embarquePlanejado - embarqueExecutado,
                descargaPlanejada,
                descargaExecutada,
                descargaPlanejada - descargaExecutada,
                atribuicoes,
                ternos
        );
    }

    public Long getId() {
        return id;
    }

    public Long getEscalaId() {
        return escalaId;
    }

    public String getNomeNavio() {
        return nomeNavio;
    }

    public String getViagemEntrada() {
        return viagemEntrada;
    }

    public StatusPlanoEstiva getStatus() {
        return status;
    }

    public int getBaias() {
        return baias;
    }

    public int getFileiras() {
        return fileiras;
    }

    public int getCamadasPorao() {
        return camadasPorao;
    }

    public int getCamadasConves() {
        return camadasConves;
    }

    public List<Integer> getTiersPorao() {
        return tiersPorao;
    }

    public List<Integer> getTiersConves() {
        return tiersConves;
    }

    public int getCapacidadeCelulas() {
        return capacidadeCelulas;
    }

    public int getEmbarquePlanejado() {
        return embarquePlanejado;
    }

    public int getEmbarqueExecutado() {
        return embarqueExecutado;
    }

    public int getEmbarquePendente() {
        return embarquePendente;
    }

    public int getDescargaPlanejada() {
        return descargaPlanejada;
    }

    public int getDescargaExecutada() {
        return descargaExecutada;
    }

    public int getDescargaPendente() {
        return descargaPendente;
    }

    public List<AtribuicaoEstivaDTO> getAtribuicoes() {
        return atribuicoes;
    }

    public List<TernoDTO> getTernos() {
        return ternos;
    }
}
