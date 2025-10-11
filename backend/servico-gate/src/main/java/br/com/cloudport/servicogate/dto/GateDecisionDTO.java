package br.com.cloudport.servicogate.dto;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GateDecisionDTO {

    private boolean autorizado;
    private String statusGate;
    private String statusDescricao;
    private Long agendamentoId;
    private String codigoAgendamento;
    private Long gatePassId;
    private String codigoGatePass;
    private String mensagem;

    public static GateDecisionDTO autorizado(StatusGate status, Agendamento agendamento, GatePass gatePass,
                                             String mensagem) {
        GateDecisionDTO dto = new GateDecisionDTO();
        dto.setAutorizado(true);
        dto.preencherContexto(status, agendamento, gatePass);
        dto.setMensagem(mensagem);
        return dto;
    }

    public static GateDecisionDTO negado(StatusGate status, Agendamento agendamento, GatePass gatePass,
                                         String mensagem) {
        GateDecisionDTO dto = new GateDecisionDTO();
        dto.setAutorizado(false);
        dto.preencherContexto(status, agendamento, gatePass);
        dto.setMensagem(mensagem);
        return dto;
    }

    private void preencherContexto(StatusGate status, Agendamento agendamento, GatePass gatePass) {
        if (status != null) {
            this.statusGate = status.name();
            this.statusDescricao = status.getDescricao();
        }
        if (agendamento != null) {
            this.agendamentoId = agendamento.getId();
            this.codigoAgendamento = agendamento.getCodigo();
        }
        if (gatePass != null) {
            this.gatePassId = gatePass.getId();
            this.codigoGatePass = gatePass.getCodigo();
        }
    }

    public boolean isAutorizado() {
        return autorizado;
    }

    public void setAutorizado(boolean autorizado) {
        this.autorizado = autorizado;
    }

    public String getStatusGate() {
        return statusGate;
    }

    public void setStatusGate(String statusGate) {
        this.statusGate = statusGate;
    }

    public String getStatusDescricao() {
        return statusDescricao;
    }

    public void setStatusDescricao(String statusDescricao) {
        this.statusDescricao = statusDescricao;
    }

    public Long getAgendamentoId() {
        return agendamentoId;
    }

    public void setAgendamentoId(Long agendamentoId) {
        this.agendamentoId = agendamentoId;
    }

    public String getCodigoAgendamento() {
        return codigoAgendamento;
    }

    public void setCodigoAgendamento(String codigoAgendamento) {
        this.codigoAgendamento = codigoAgendamento;
    }

    public Long getGatePassId() {
        return gatePassId;
    }

    public void setGatePassId(Long gatePassId) {
        this.gatePassId = gatePassId;
    }

    public String getCodigoGatePass() {
        return codigoGatePass;
    }

    public void setCodigoGatePass(String codigoGatePass) {
        this.codigoGatePass = codigoGatePass;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
