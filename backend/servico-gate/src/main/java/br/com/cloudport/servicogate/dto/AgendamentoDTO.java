package br.com.cloudport.servicogate.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AgendamentoDTO {

    private Long id;
    private String codigo;
    private String tipoOperacao;
    private String tipoOperacaoDescricao;
    private String status;
    private String statusDescricao;
    private Long transportadoraId;
    private String transportadoraNome;
    private Long motoristaId;
    private String motoristaNome;
    private Long veiculoId;
    private String placaVeiculo;
    private Long janelaAtendimentoId;
    private LocalDate dataJanela;
    private LocalTime horaInicioJanela;
    private LocalTime horaFimJanela;
    private LocalDateTime horarioPrevistoChegada;
    private LocalDateTime horarioPrevistoSaida;
    private LocalDateTime horarioRealChegada;
    private LocalDateTime horarioRealSaida;
    private String observacoes;
    private List<DocumentoAgendamentoDTO> documentos;
    private GatePassDTO gatePass;

    public AgendamentoDTO() {
    }

    public AgendamentoDTO(Long id, String codigo, String tipoOperacao, String tipoOperacaoDescricao,
                           String status, String statusDescricao, Long transportadoraId,
                           String transportadoraNome, Long motoristaId, String motoristaNome,
                           Long veiculoId, String placaVeiculo, Long janelaAtendimentoId,
                           LocalDate dataJanela, LocalTime horaInicioJanela, LocalTime horaFimJanela,
                           LocalDateTime horarioPrevistoChegada, LocalDateTime horarioPrevistoSaida,
                           LocalDateTime horarioRealChegada, LocalDateTime horarioRealSaida,
                           String observacoes, List<DocumentoAgendamentoDTO> documentos,
                           GatePassDTO gatePass) {
        this.id = id;
        this.codigo = codigo;
        this.tipoOperacao = tipoOperacao;
        this.tipoOperacaoDescricao = tipoOperacaoDescricao;
        this.status = status;
        this.statusDescricao = statusDescricao;
        this.transportadoraId = transportadoraId;
        this.transportadoraNome = transportadoraNome;
        this.motoristaId = motoristaId;
        this.motoristaNome = motoristaNome;
        this.veiculoId = veiculoId;
        this.placaVeiculo = placaVeiculo;
        this.janelaAtendimentoId = janelaAtendimentoId;
        this.dataJanela = dataJanela;
        this.horaInicioJanela = horaInicioJanela;
        this.horaFimJanela = horaFimJanela;
        this.horarioPrevistoChegada = horarioPrevistoChegada;
        this.horarioPrevistoSaida = horarioPrevistoSaida;
        this.horarioRealChegada = horarioRealChegada;
        this.horarioRealSaida = horarioRealSaida;
        this.observacoes = observacoes;
        this.documentos = documentos;
        this.gatePass = gatePass;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getTipoOperacaoDescricao() {
        return tipoOperacaoDescricao;
    }

    public void setTipoOperacaoDescricao(String tipoOperacaoDescricao) {
        this.tipoOperacaoDescricao = tipoOperacaoDescricao;
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

    public Long getTransportadoraId() {
        return transportadoraId;
    }

    public void setTransportadoraId(Long transportadoraId) {
        this.transportadoraId = transportadoraId;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }

    public Long getMotoristaId() {
        return motoristaId;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }

    public String getMotoristaNome() {
        return motoristaNome;
    }

    public void setMotoristaNome(String motoristaNome) {
        this.motoristaNome = motoristaNome;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
    }

    public void setPlacaVeiculo(String placaVeiculo) {
        this.placaVeiculo = placaVeiculo;
    }

    public Long getJanelaAtendimentoId() {
        return janelaAtendimentoId;
    }

    public void setJanelaAtendimentoId(Long janelaAtendimentoId) {
        this.janelaAtendimentoId = janelaAtendimentoId;
    }

    public LocalDate getDataJanela() {
        return dataJanela;
    }

    public void setDataJanela(LocalDate dataJanela) {
        this.dataJanela = dataJanela;
    }

    public LocalTime getHoraInicioJanela() {
        return horaInicioJanela;
    }

    public void setHoraInicioJanela(LocalTime horaInicioJanela) {
        this.horaInicioJanela = horaInicioJanela;
    }

    public LocalTime getHoraFimJanela() {
        return horaFimJanela;
    }

    public void setHoraFimJanela(LocalTime horaFimJanela) {
        this.horaFimJanela = horaFimJanela;
    }

    public LocalDateTime getHorarioPrevistoChegada() {
        return horarioPrevistoChegada;
    }

    public void setHorarioPrevistoChegada(LocalDateTime horarioPrevistoChegada) {
        this.horarioPrevistoChegada = horarioPrevistoChegada;
    }

    public LocalDateTime getHorarioPrevistoSaida() {
        return horarioPrevistoSaida;
    }

    public void setHorarioPrevistoSaida(LocalDateTime horarioPrevistoSaida) {
        this.horarioPrevistoSaida = horarioPrevistoSaida;
    }

    public LocalDateTime getHorarioRealChegada() {
        return horarioRealChegada;
    }

    public void setHorarioRealChegada(LocalDateTime horarioRealChegada) {
        this.horarioRealChegada = horarioRealChegada;
    }

    public LocalDateTime getHorarioRealSaida() {
        return horarioRealSaida;
    }

    public void setHorarioRealSaida(LocalDateTime horarioRealSaida) {
        this.horarioRealSaida = horarioRealSaida;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<DocumentoAgendamentoDTO> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<DocumentoAgendamentoDTO> documentos) {
        this.documentos = documentos;
    }

    public GatePassDTO getGatePass() {
        return gatePass;
    }

    public void setGatePass(GatePassDTO gatePass) {
        this.gatePass = gatePass;
    }
}
