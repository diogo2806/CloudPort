package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DashboardVisibilidadeDTO {

    private LocalDateTime atualizadoEm;
    private List<AlertaDTO> alertasAtivos;
    private List<StatusNavioDTO> naviosEmOperacao;
    private OcupacaoPatioDTO patio;
    private ThroughputGateDTO gate;
    private List<ConteinerBuscaDTO> conteineresCriticos;

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public List<AlertaDTO> getAlertasAtivos() {
        return alertasAtivos;
    }

    public void setAlertasAtivos(List<AlertaDTO> alertasAtivos) {
        this.alertasAtivos = alertasAtivos;
    }

    public List<StatusNavioDTO> getNaviosEmOperacao() {
        return naviosEmOperacao;
    }

    public void setNaviosEmOperacao(List<StatusNavioDTO> naviosEmOperacao) {
        this.naviosEmOperacao = naviosEmOperacao;
    }

    public OcupacaoPatioDTO getPatio() {
        return patio;
    }

    public void setPatio(OcupacaoPatioDTO patio) {
        this.patio = patio;
    }

    public ThroughputGateDTO getGate() {
        return gate;
    }

    public void setGate(ThroughputGateDTO gate) {
        this.gate = gate;
    }

    public List<ConteinerBuscaDTO> getConteineresCriticos() {
        return conteineresCriticos;
    }

    public void setConteineresCriticos(List<ConteinerBuscaDTO> conteineresCriticos) {
        this.conteineresCriticos = conteineresCriticos;
    }
}
