package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.StatusPlanoEstiva;
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
    private final int totalPlanejado;
    private final int totalEmbarcado;
    private final int totalPendente;
    private final double ocupacaoPercentual;
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
                                 int totalPlanejado,
                                 int totalEmbarcado,
                                 int totalPendente,
                                 double ocupacaoPercentual,
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
        this.totalPlanejado = totalPlanejado;
        this.totalEmbarcado = totalEmbarcado;
        this.totalPendente = totalPendente;
        this.ocupacaoPercentual = ocupacaoPercentual;
        this.atribuicoes = atribuicoes;
    }

    public static PlanoEstivaDetalheDTO deEntidade(PlanoEstiva plano) {
        List<AtribuicaoEstivaDTO> atribuicoes = plano.getAtribuicoes().stream()
                .map(AtribuicaoEstivaDTO::deEntidade)
                .collect(Collectors.toList());
        int totalPlanejado = atribuicoes.size();
        int totalEmbarcado = (int) atribuicoes.stream().filter(AtribuicaoEstivaDTO::isEmbarcado).count();
        int capacidade = plano.capacidadeCelulas();
        double ocupacao = capacidade == 0 ? 0d
                : Math.round((totalPlanejado * 10000d) / capacidade) / 100d;
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
                capacidade,
                totalPlanejado,
                totalEmbarcado,
                totalPlanejado - totalEmbarcado,
                ocupacao,
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

    public int getTotalPlanejado() {
        return totalPlanejado;
    }

    public int getTotalEmbarcado() {
        return totalEmbarcado;
    }

    public int getTotalPendente() {
        return totalPendente;
    }

    public double getOcupacaoPercentual() {
        return ocupacaoPercentual;
    }

    public List<AtribuicaoEstivaDTO> getAtribuicoes() {
        return atribuicoes;
    }
}
