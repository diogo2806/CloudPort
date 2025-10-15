package br.com.cloudport.servicogate.app.cidadao.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AgendamentoRequest {

    @NotBlank
    @Size(max = 40)
    private String codigo;

    @NotBlank
    private String tipoOperacao;

    @NotBlank
    private String status;

    @NotNull
    private Long transportadoraId;

    @NotNull
    private Long motoristaId;

    @NotNull
    private Long veiculoId;

    @NotNull
    private Long janelaAtendimentoId;

    @NotNull
    @Future(message = "Horário previsto de chegada deve estar no futuro")
    private LocalDateTime horarioPrevistoChegada;

    @NotNull
    @Future(message = "Horário previsto de saída deve estar no futuro")
    private LocalDateTime horarioPrevistoSaida;

    @Size(max = 500)
    private String observacoes;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTransportadoraId() {
        return transportadoraId;
    }

    public void setTransportadoraId(Long transportadoraId) {
        this.transportadoraId = transportadoraId;
    }

    public Long getMotoristaId() {
        return motoristaId;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public Long getJanelaAtendimentoId() {
        return janelaAtendimentoId;
    }

    public void setJanelaAtendimentoId(Long janelaAtendimentoId) {
        this.janelaAtendimentoId = janelaAtendimentoId;
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

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
