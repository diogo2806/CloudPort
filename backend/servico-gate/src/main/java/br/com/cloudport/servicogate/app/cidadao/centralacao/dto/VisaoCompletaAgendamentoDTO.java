package br.com.cloudport.servicogate.app.cidadao.centralacao.dto;

import java.util.ArrayList;
import java.util.List;

public class VisaoCompletaAgendamentoDTO {

    private Long agendamentoId;
    private String codigo;
    private String status;
    private String statusDescricao;
    private String tipoOperacaoDescricao;
    private String horarioPrevistoChegada;
    private String horarioPrevistoSaida;
    private String placaVeiculo;
    private String transportadoraNome;
    private String motoristaNome;
    private String janelaData;
    private String janelaHoraInicio;
    private String janelaHoraFim;
    private String mensagemOrientacao;
    private AcaoAgendamentoDTO acaoPrincipal;
    private List<DocumentoPendenteDTO> documentosPendentes = new ArrayList<>();

    public VisaoCompletaAgendamentoDTO() {
    }

    public VisaoCompletaAgendamentoDTO(Long agendamentoId,
                                        String codigo,
                                        String status,
                                        String statusDescricao,
                                        String tipoOperacaoDescricao,
                                        String horarioPrevistoChegada,
                                        String horarioPrevistoSaida,
                                        String placaVeiculo,
                                        String transportadoraNome,
                                        String motoristaNome,
                                        String janelaData,
                                        String janelaHoraInicio,
                                        String janelaHoraFim,
                                        String mensagemOrientacao,
                                        AcaoAgendamentoDTO acaoPrincipal,
                                        List<DocumentoPendenteDTO> documentosPendentes) {
        this.agendamentoId = agendamentoId;
        this.codigo = codigo;
        this.status = status;
        this.statusDescricao = statusDescricao;
        this.tipoOperacaoDescricao = tipoOperacaoDescricao;
        this.horarioPrevistoChegada = horarioPrevistoChegada;
        this.horarioPrevistoSaida = horarioPrevistoSaida;
        this.placaVeiculo = placaVeiculo;
        this.transportadoraNome = transportadoraNome;
        this.motoristaNome = motoristaNome;
        this.janelaData = janelaData;
        this.janelaHoraInicio = janelaHoraInicio;
        this.janelaHoraFim = janelaHoraFim;
        this.mensagemOrientacao = mensagemOrientacao;
        this.acaoPrincipal = acaoPrincipal;
        if (documentosPendentes != null) {
            this.documentosPendentes = documentosPendentes;
        }
    }

    public Long getAgendamentoId() {
        return agendamentoId;
    }

    public void setAgendamentoId(Long agendamentoId) {
        this.agendamentoId = agendamentoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescricao() {
        return statusDescricao;
    }

    public void setStatusDescricao(String statusDescricao) {
        this.statusDescricao = statusDescricao;
    }

    public String getTipoOperacaoDescricao() {
        return tipoOperacaoDescricao;
    }

    public void setTipoOperacaoDescricao(String tipoOperacaoDescricao) {
        this.tipoOperacaoDescricao = tipoOperacaoDescricao;
    }

    public String getHorarioPrevistoChegada() {
        return horarioPrevistoChegada;
    }

    public void setHorarioPrevistoChegada(String horarioPrevistoChegada) {
        this.horarioPrevistoChegada = horarioPrevistoChegada;
    }

    public String getHorarioPrevistoSaida() {
        return horarioPrevistoSaida;
    }

    public void setHorarioPrevistoSaida(String horarioPrevistoSaida) {
        this.horarioPrevistoSaida = horarioPrevistoSaida;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
    }

    public void setPlacaVeiculo(String placaVeiculo) {
        this.placaVeiculo = placaVeiculo;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }

    public String getMotoristaNome() {
        return motoristaNome;
    }

    public void setMotoristaNome(String motoristaNome) {
        this.motoristaNome = motoristaNome;
    }

    public String getJanelaData() {
        return janelaData;
    }

    public void setJanelaData(String janelaData) {
        this.janelaData = janelaData;
    }

    public String getJanelaHoraInicio() {
        return janelaHoraInicio;
    }

    public void setJanelaHoraInicio(String janelaHoraInicio) {
        this.janelaHoraInicio = janelaHoraInicio;
    }

    public String getJanelaHoraFim() {
        return janelaHoraFim;
    }

    public void setJanelaHoraFim(String janelaHoraFim) {
        this.janelaHoraFim = janelaHoraFim;
    }

    public String getMensagemOrientacao() {
        return mensagemOrientacao;
    }

    public void setMensagemOrientacao(String mensagemOrientacao) {
        this.mensagemOrientacao = mensagemOrientacao;
    }

    public AcaoAgendamentoDTO getAcaoPrincipal() {
        return acaoPrincipal;
    }

    public void setAcaoPrincipal(AcaoAgendamentoDTO acaoPrincipal) {
        this.acaoPrincipal = acaoPrincipal;
    }

    public List<DocumentoPendenteDTO> getDocumentosPendentes() {
        return documentosPendentes;
    }

    public void setDocumentosPendentes(List<DocumentoPendenteDTO> documentosPendentes) {
        this.documentosPendentes = documentosPendentes != null ? documentosPendentes : new ArrayList<>();
    }
}
