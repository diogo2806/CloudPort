package br.com.cloudport.servicogate.app.gestor.dto;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GateDecisionDTO {

    private boolean autorizado;
    private String statusGate;
    private String statusDescricao;
    private Long agendamentoId;
    private String codigoAgendamento;
    private Long gatePassId;
    private String codigoGatePass;
    private String tokenGatePass;
    private UUID reservaCargaGeralId;
    private String statusReservaCargaGeral;
    private String estagioConfirmacaoCargaGeral;
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

    public static GateDecisionDTO pendenteBarcodeConfirmacao(GatePass gatePass) {
        GateDecisionDTO dto = new GateDecisionDTO();
        dto.setAutorizado(false);
        dto.setStatusGate(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE.name());
        dto.setStatusDescricao(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE.getDescricao());
        dto.setGatePassId(gatePass.getId());
        dto.setCodigoGatePass(gatePass.getCodigo());
        dto.setTokenGatePass(gatePass.getToken());
        dto.setMensagem("Aguardando confirmação de barcode do operador DMT. Token enviado para dispositivo móvel.");
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

    public String getTokenGatePass() {
        return tokenGatePass;
    }

    public void setTokenGatePass(String tokenGatePass) {
        this.tokenGatePass = tokenGatePass;
    }

    public UUID getReservaCargaGeralId() {
        return reservaCargaGeralId;
    }

    public void setReservaCargaGeralId(UUID reservaCargaGeralId) {
        this.reservaCargaGeralId = reservaCargaGeralId;
    }

    public String getStatusReservaCargaGeral() {
        return statusReservaCargaGeral;
    }

    public void setStatusReservaCargaGeral(String statusReservaCargaGeral) {
        this.statusReservaCargaGeral = statusReservaCargaGeral;
    }

    public String getEstagioConfirmacaoCargaGeral() {
        return estagioConfirmacaoCargaGeral;
    }

    public void setEstagioConfirmacaoCargaGeral(String estagioConfirmacaoCargaGeral) {
        this.estagioConfirmacaoCargaGeral = estagioConfirmacaoCargaGeral;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
