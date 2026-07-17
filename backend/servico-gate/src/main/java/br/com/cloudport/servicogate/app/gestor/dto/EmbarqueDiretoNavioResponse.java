package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;

public class EmbarqueDiretoNavioResponse {

    private final Long agendamentoId;
    private final String codigoConteiner;
    private final Long gatePassId;
    private final Long atribuicaoEstivaId;
    private final Long planoEstivaId;
    private final int baia;
    private final int fileira;
    private final int camada;
    private final LocalDateTime entradaGateEm;
    private final LocalDateTime embarcadoEm;
    private final LocalDateTime saidaGateEm;
    private final boolean passouPeloPatio;
    private final String statusGate;
    private final String statusAgendamento;
    private final String mensagem;

    public EmbarqueDiretoNavioResponse(Long agendamentoId,
                                       String codigoConteiner,
                                       Long gatePassId,
                                       Long atribuicaoEstivaId,
                                       Long planoEstivaId,
                                       int baia,
                                       int fileira,
                                       int camada,
                                       LocalDateTime entradaGateEm,
                                       LocalDateTime embarcadoEm,
                                       LocalDateTime saidaGateEm,
                                       boolean passouPeloPatio,
                                       String statusGate,
                                       String statusAgendamento,
                                       String mensagem) {
        this.agendamentoId = agendamentoId;
        this.codigoConteiner = codigoConteiner;
        this.gatePassId = gatePassId;
        this.atribuicaoEstivaId = atribuicaoEstivaId;
        this.planoEstivaId = planoEstivaId;
        this.baia = baia;
        this.fileira = fileira;
        this.camada = camada;
        this.entradaGateEm = entradaGateEm;
        this.embarcadoEm = embarcadoEm;
        this.saidaGateEm = saidaGateEm;
        this.passouPeloPatio = passouPeloPatio;
        this.statusGate = statusGate;
        this.statusAgendamento = statusAgendamento;
        this.mensagem = mensagem;
    }

    public Long getAgendamentoId() { return agendamentoId; }
    public String getCodigoConteiner() { return codigoConteiner; }
    public Long getGatePassId() { return gatePassId; }
    public Long getAtribuicaoEstivaId() { return atribuicaoEstivaId; }
    public Long getPlanoEstivaId() { return planoEstivaId; }
    public int getBaia() { return baia; }
    public int getFileira() { return fileira; }
    public int getCamada() { return camada; }
    public LocalDateTime getEntradaGateEm() { return entradaGateEm; }
    public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
    public LocalDateTime getSaidaGateEm() { return saidaGateEm; }
    public boolean isPassouPeloPatio() { return passouPeloPatio; }
    public String getStatusGate() { return statusGate; }
    public String getStatusAgendamento() { return statusAgendamento; }
    public String getMensagem() { return mensagem; }
}
