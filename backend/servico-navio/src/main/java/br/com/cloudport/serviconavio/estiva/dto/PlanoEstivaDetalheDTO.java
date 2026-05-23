package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.StatusPlanoEstiva;
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
    private final int camadas;
    private final int capacidadeCelulas;
    private final int embarquePlanejado;
    private final int embarqueExecutado;
    private final int embarquePendente;
    private final int descargaPlanejada;
    private final int descargaExecutada;
    private final int descargaPendente;
    private final List<AtribuicaoEstivaDTO> atribuicoes;

    public PlanoEstivaDetalheDTO(Long id,
                                 Long escalaId,
                                 String nomeNavio,
                                 String viagemEntrada,
                                 StatusPlanoEstiva status,
                                 int baias,
                                 int fileiras,
                                 int camadas,
                                 int capacidadeCelulas,
                                 int embarquePlanejado,
                                 int embarqueExecutado,
                                 int embarquePendente,
                                 int descargaPlanejada,
                                 int descargaExecutada,
                                 int descargaPendente,
                                 List<AtribuicaoEstivaDTO> atribuicoes) {
        this.id = id;
        this.escalaId = escalaId;
        this.nomeNavio = nomeNavio;
        this.viagemEntrada = viagemEntrada;
        this.status = status;
        this.baias = baias;
        this.fileiras = fileiras;
        this.camadas = camadas;
        this.capacidadeCelulas = capacidadeCelulas;
        this.embarquePlanejado = embarquePlanejado;
        this.embarqueExecutado = embarqueExecutado;
        this.embarquePendente = embarquePendente;
        this.descargaPlanejada = descargaPlanejada;
        this.descargaExecutada = descargaExecutada;
        this.descargaPendente = descargaPendente;
        this.atribuicoes = atribuicoes;
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

        Escala escala = plano.getEscala();
        return new PlanoEstivaDetalheDTO(
                plano.getId(),
                escala.getId(),
                escala.getNavio().getNome(),
                escala.getViagemEntrada(),
                plano.getStatus(),
                plano.getBaias(),
                plano.getFileiras(),
                plano.getCamadas(),
                plano.capacidadeCelulas(),
                embarquePlanejado,
                embarqueExecutado,
                embarquePlanejado - embarqueExecutado,
                descargaPlanejada,
                descargaExecutada,
                descargaPlanejada - descargaExecutada,
                atribuicoes
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

    public int getCamadas() {
        return camadas;
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
}
