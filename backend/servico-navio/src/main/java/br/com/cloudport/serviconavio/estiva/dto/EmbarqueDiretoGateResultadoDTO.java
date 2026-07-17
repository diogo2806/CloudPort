package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.estiva.entidade.AtribuicaoEstiva;
import java.time.LocalDateTime;

public class EmbarqueDiretoGateResultadoDTO {

    private final Long atribuicaoEstivaId;
    private final Long planoEstivaId;
    private final String codigoConteiner;
    private final int baia;
    private final int fileira;
    private final int camada;
    private final LocalDateTime embarcadoEm;

    public EmbarqueDiretoGateResultadoDTO(Long atribuicaoEstivaId,
                                          Long planoEstivaId,
                                          String codigoConteiner,
                                          int baia,
                                          int fileira,
                                          int camada,
                                          LocalDateTime embarcadoEm) {
        this.atribuicaoEstivaId = atribuicaoEstivaId;
        this.planoEstivaId = planoEstivaId;
        this.codigoConteiner = codigoConteiner;
        this.baia = baia;
        this.fileira = fileira;
        this.camada = camada;
        this.embarcadoEm = embarcadoEm;
    }

    public static EmbarqueDiretoGateResultadoDTO deEntidade(AtribuicaoEstiva atribuicao) {
        return new EmbarqueDiretoGateResultadoDTO(
                atribuicao.getId(),
                atribuicao.getPlano().getId(),
                atribuicao.getCodigoConteiner(),
                atribuicao.getBaia(),
                atribuicao.getFileira(),
                atribuicao.getCamada(),
                atribuicao.getEmbarcadoEm());
    }

    public Long getAtribuicaoEstivaId() { return atribuicaoEstivaId; }
    public Long getPlanoEstivaId() { return planoEstivaId; }
    public String getCodigoConteiner() { return codigoConteiner; }
    public int getBaia() { return baia; }
    public int getFileira() { return fileira; }
    public int getCamada() { return camada; }
    public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
}
