package br.com.cloudport.servicogate.integration.hardware;

import br.com.cloudport.servicogate.dto.GateDecisionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HardwareDecisionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean autorizado;
    private String statusGate;
    private String statusDescricao;
    private Long agendamentoId;
    private String codigoAgendamento;
    private Long gatePassId;
    private String codigoGatePass;
    private String mensagem;
    private String placa;
    private String qrCode;
    private LocalDateTime timestamp;
    private String origem;
    private String dispositivo;

    public static HardwareDecisionMessage from(GateDecisionDTO decision, HardwareEventMessage event) {
        HardwareDecisionMessage message = new HardwareDecisionMessage();
        if (decision != null) {
            message.setAutorizado(decision.isAutorizado());
            message.setStatusGate(decision.getStatusGate());
            message.setStatusDescricao(decision.getStatusDescricao());
            message.setAgendamentoId(decision.getAgendamentoId());
            message.setCodigoAgendamento(decision.getCodigoAgendamento());
            message.setGatePassId(decision.getGatePassId());
            message.setCodigoGatePass(decision.getCodigoGatePass());
            message.setMensagem(decision.getMensagem());
        }
        if (event != null) {
            message.setPlaca(event.getPlaca());
            message.setQrCode(event.getQrCode());
            message.setTimestamp(event.getTimestamp());
            message.setOrigem(event.getOrigem());
            message.setDispositivo(event.getDispositivo());
        }
        return message;
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

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
}
