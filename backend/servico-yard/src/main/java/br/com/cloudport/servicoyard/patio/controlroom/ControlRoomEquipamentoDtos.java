package br.com.cloudport.servicoyard.patio.controlroom;

import java.time.LocalDateTime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public final class ControlRoomEquipamentoDtos {

    private ControlRoomEquipamentoDtos() {
    }

    public record Resumo(
            long totalEquipamentos,
            long operacionais,
            long emManutencao,
            long indisponiveis,
            long conectados,
            long telemetriaAtrasada,
            long alarmesAtivos,
            long comandosPendentes,
            long indisponibilidadesAbertas,
            LocalDateTime atualizadoEm
    ) {
    }

    public record Equipamento(
            Long id,
            String identificador,
            String tipoEquipamento,
            String statusOperacional,
            Integer linha,
            Integer coluna,
            String conectividade,
            String dispositivo,
            String protocolo,
            String firmware,
            LocalDateTime ultimoHeartbeatEm,
            Double latitude,
            Double longitude,
            Double coordenadaX,
            Double coordenadaY,
            Double heading,
            String posicaoMaisProxima,
            Boolean dentroDaPosicao,
            String statusVmt,
            Long workInstructionAtualId,
            Long sequenciaTelemetria,
            LocalDateTime capturadoEm,
            LocalDateTime recebidoEm,
            long alarmesAtivos,
            Long indisponibilidadeAbertaId
    ) {
    }

    public record HistoricoTelemetria(
            Long id,
            String equipamento,
            Double latitude,
            Double longitude,
            Double coordenadaX,
            Double coordenadaY,
            Double heading,
            String posicaoMaisProxima,
            Integer distanciaPosicaoCentimetros,
            Boolean dentroDaPosicao,
            String origem,
            String operadorVmt,
            String statusVmt,
            Long workInstructionAtualId,
            Long sequencia,
            LocalDateTime capturadoEm,
            LocalDateTime recebidoEm
    ) {
    }

    public record Alarme(
            Long id,
            String equipamento,
            String tipoEquipamento,
            String tipo,
            String severidade,
            String status,
            String mensagem,
            String origem,
            Map<String, Object> detalhes,
            LocalDateTime abertoEm,
            LocalDateTime reconhecidoEm,
            String reconhecidoPor,
            LocalDateTime resolvidoEm,
            String resolvidoPor
    ) {
    }

    public record Comando(
            Long id,
            String equipamento,
            String dispositivo,
            String tipo,
            String status,
            Map<String, Object> parametros,
            String mensagem,
            String solicitadoPor,
            String correlationId,
            LocalDateTime criadoEm,
            LocalDateTime enviadoEm,
            LocalDateTime confirmadoEm,
            String retornoDispositivo,
            Long sequenciaDispositivo
    ) {
    }

    public record Indisponibilidade(
            Long id,
            String equipamento,
            String tipoEquipamento,
            String motivo,
            String observacao,
            LocalDateTime inicioEm,
            LocalDateTime fimEm,
            String abertoPor,
            String encerradoPor,
            Long comandoOrigemId
    ) {
    }

    public record Dispositivo(
            Long id,
            String identificador,
            String equipamento,
            String tipoEquipamento,
            String protocolo,
            String statusIntegracao,
            String firmware,
            String enderecoRede,
            Long ultimaSequencia,
            LocalDateTime ultimoHeartbeatEm,
            LocalDateTime atualizadoEm
    ) {
    }

    public static class ComandoRequisicao {
        @NotBlank
        private String tipo;
        private Map<String, Object> parametros;
        private String mensagem;
        private String correlationId;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Map<String, Object> getParametros() { return parametros; }
        public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }
        public String getMensagem() { return mensagem; }
        public void setMensagem(String mensagem) { this.mensagem = mensagem; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    }

    public static class ConfirmacaoComandoRequisicao {
        @NotBlank
        private String status;
        private String retorno;
        @PositiveOrZero
        private Long sequenciaDispositivo;
        private LocalDateTime confirmadoEm;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRetorno() { return retorno; }
        public void setRetorno(String retorno) { this.retorno = retorno; }
        public Long getSequenciaDispositivo() { return sequenciaDispositivo; }
        public void setSequenciaDispositivo(Long sequenciaDispositivo) { this.sequenciaDispositivo = sequenciaDispositivo; }
        public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
        public void setConfirmadoEm(LocalDateTime confirmadoEm) { this.confirmadoEm = confirmadoEm; }
    }

    public static class IndisponibilidadeRequisicao {
        @NotBlank
        private String motivo;
        private String observacao;

        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public String getObservacao() { return observacao; }
        public void setObservacao(String observacao) { this.observacao = observacao; }
    }

    public static class EncerramentoIndisponibilidadeRequisicao {
        private String observacao;

        public String getObservacao() { return observacao; }
        public void setObservacao(String observacao) { this.observacao = observacao; }
    }

    public static class HeartbeatRequisicao {
        @NotBlank
        private String equipamento;
        @NotBlank
        private String protocolo;
        private String firmware;
        private String enderecoRede;
        private String statusIntegracao;
        @NotNull
        @PositiveOrZero
        private Long sequencia;
        @NotNull
        private LocalDateTime capturadoEm;

        public String getEquipamento() { return equipamento; }
        public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
        public String getProtocolo() { return protocolo; }
        public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
        public String getFirmware() { return firmware; }
        public void setFirmware(String firmware) { this.firmware = firmware; }
        public String getEnderecoRede() { return enderecoRede; }
        public void setEnderecoRede(String enderecoRede) { this.enderecoRede = enderecoRede; }
        public String getStatusIntegracao() { return statusIntegracao; }
        public void setStatusIntegracao(String statusIntegracao) { this.statusIntegracao = statusIntegracao; }
        public Long getSequencia() { return sequencia; }
        public void setSequencia(Long sequencia) { this.sequencia = sequencia; }
        public LocalDateTime getCapturadoEm() { return capturadoEm; }
        public void setCapturadoEm(LocalDateTime capturadoEm) { this.capturadoEm = capturadoEm; }
    }
}
