package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.ModoDispatch;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.StatusEtapa;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.TipoEscopo;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.TipoEtapa;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

public final class DispatchDtos {

    private DispatchDtos() {
    }

    public static class AutoDispatchRequest {
        @NotNull
        private Long workQueueId;
        @NotNull
        private Long equipamentoPatioId;
        private Long ordemTrabalhoPatioId;
        @Size(max = 40)
        private String codigoUnidade;
        @NotBlank
        @Size(max = 120)
        private String operador;
        @NotBlank
        @Size(max = 40)
        private String faseVisita;
        @Size(max = 80)
        private String pow;
        @Size(max = 80)
        private String pool;
        @NotBlank
        @Size(max = 120)
        private String chaveIdempotencia;
        @NotBlank
        @Size(max = 500)
        private String motivo;
        @Size(max = 80)
        private String origemAcao;
        @Size(max = 100)
        private String correlationId;

        public Long getWorkQueueId() { return workQueueId; }
        public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
        public Long getEquipamentoPatioId() { return equipamentoPatioId; }
        public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
        public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
        public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
        public String getCodigoUnidade() { return codigoUnidade; }
        public void setCodigoUnidade(String codigoUnidade) { this.codigoUnidade = codigoUnidade; }
        public String getOperador() { return operador; }
        public void setOperador(String operador) { this.operador = operador; }
        public String getFaseVisita() { return faseVisita; }
        public void setFaseVisita(String faseVisita) { this.faseVisita = faseVisita; }
        public String getPow() { return pow; }
        public void setPow(String pow) { this.pow = pow; }
        public String getPool() { return pool; }
        public void setPool(String pool) { this.pool = pool; }
        public String getChaveIdempotencia() { return chaveIdempotencia; }
        public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public String getOrigemAcao() { return origemAcao; }
        public void setOrigemAcao(String origemAcao) { this.origemAcao = origemAcao; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    }

    public static class EtapaRequest {
        @NotNull
        private StatusEtapa statusDestino;
        @NotBlank
        @Size(max = 120)
        private String operador;
        @Size(max = 500)
        private String evidencia;
        @NotBlank
        @Size(max = 120)
        private String chaveIdempotencia;

        public StatusEtapa getStatusDestino() { return statusDestino; }
        public void setStatusDestino(StatusEtapa statusDestino) { this.statusDestino = statusDestino; }
        public String getOperador() { return operador; }
        public void setOperador(String operador) { this.operador = operador; }
        public String getEvidencia() { return evidencia; }
        public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
        public String getChaveIdempotencia() { return chaveIdempotencia; }
        public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    }

    public static class ConfiguracaoRequest {
        @NotNull
        private TipoEscopo tipoEscopo;
        @NotBlank
        @Size(max = 120)
        private String valorEscopo;
        @NotNull
        private TipoEquipamento tipoEquipamento;
        @NotNull
        private ModoDispatch modo;
        @PositiveOrZero
        private Double pesoPrioridade;
        @PositiveOrZero
        private Double pesoDistancia;
        @PositiveOrZero
        private Double pesoAtraso;
        @PositiveOrZero
        private Double pesoCongestionamento;
        @Positive
        private Double velocidadeMediaKmh;
        @PositiveOrZero
        private Integer tempoColetaSegundos;
        @PositiveOrZero
        private Integer tempoEntregaSegundos;
        @Positive
        private Integer toleranciaTelemetriaSegundos;
        @Positive
        private Integer capacidadeSimultanea;
        @Positive
        private Integer limiteRegionalChe;
        private Boolean selecionarAuxiliar;
        private Boolean permitirOverride;
        private LocalDateTime vigenteDe;
        private LocalDateTime vigenteAte;
        @NotBlank
        @Size(max = 500)
        private String motivo;

        public TipoEscopo getTipoEscopo() { return tipoEscopo; }
        public void setTipoEscopo(TipoEscopo tipoEscopo) { this.tipoEscopo = tipoEscopo; }
        public String getValorEscopo() { return valorEscopo; }
        public void setValorEscopo(String valorEscopo) { this.valorEscopo = valorEscopo; }
        public TipoEquipamento getTipoEquipamento() { return tipoEquipamento; }
        public void setTipoEquipamento(TipoEquipamento tipoEquipamento) { this.tipoEquipamento = tipoEquipamento; }
        public ModoDispatch getModo() { return modo; }
        public void setModo(ModoDispatch modo) { this.modo = modo; }
        public Double getPesoPrioridade() { return pesoPrioridade; }
        public void setPesoPrioridade(Double pesoPrioridade) { this.pesoPrioridade = pesoPrioridade; }
        public Double getPesoDistancia() { return pesoDistancia; }
        public void setPesoDistancia(Double pesoDistancia) { this.pesoDistancia = pesoDistancia; }
        public Double getPesoAtraso() { return pesoAtraso; }
        public void setPesoAtraso(Double pesoAtraso) { this.pesoAtraso = pesoAtraso; }
        public Double getPesoCongestionamento() { return pesoCongestionamento; }
        public void setPesoCongestionamento(Double pesoCongestionamento) { this.pesoCongestionamento = pesoCongestionamento; }
        public Double getVelocidadeMediaKmh() { return velocidadeMediaKmh; }
        public void setVelocidadeMediaKmh(Double velocidadeMediaKmh) { this.velocidadeMediaKmh = velocidadeMediaKmh; }
        public Integer getTempoColetaSegundos() { return tempoColetaSegundos; }
        public void setTempoColetaSegundos(Integer tempoColetaSegundos) { this.tempoColetaSegundos = tempoColetaSegundos; }
        public Integer getTempoEntregaSegundos() { return tempoEntregaSegundos; }
        public void setTempoEntregaSegundos(Integer tempoEntregaSegundos) { this.tempoEntregaSegundos = tempoEntregaSegundos; }
        public Integer getToleranciaTelemetriaSegundos() { return toleranciaTelemetriaSegundos; }
        public void setToleranciaTelemetriaSegundos(Integer toleranciaTelemetriaSegundos) { this.toleranciaTelemetriaSegundos = toleranciaTelemetriaSegundos; }
        public Integer getCapacidadeSimultanea() { return capacidadeSimultanea; }
        public void setCapacidadeSimultanea(Integer capacidadeSimultanea) { this.capacidadeSimultanea = capacidadeSimultanea; }
        public Integer getLimiteRegionalChe() { return limiteRegionalChe; }
        public void setLimiteRegionalChe(Integer limiteRegionalChe) { this.limiteRegionalChe = limiteRegionalChe; }
        public Boolean getSelecionarAuxiliar() { return selecionarAuxiliar; }
        public void setSelecionarAuxiliar(Boolean selecionarAuxiliar) { this.selecionarAuxiliar = selecionarAuxiliar; }
        public Boolean getPermitirOverride() { return permitirOverride; }
        public void setPermitirOverride(Boolean permitirOverride) { this.permitirOverride = permitirOverride; }
        public LocalDateTime getVigenteDe() { return vigenteDe; }
        public void setVigenteDe(LocalDateTime vigenteDe) { this.vigenteDe = vigenteDe; }
        public LocalDateTime getVigenteAte() { return vigenteAte; }
        public void setVigenteAte(LocalDateTime vigenteAte) { this.vigenteAte = vigenteAte; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }

    public record Configuracao(
            Long id,
            TipoEscopo tipoEscopo,
            String valorEscopo,
            TipoEquipamento tipoEquipamento,
            Long versao,
            String status,
            ModoDispatch modo,
            double pesoPrioridade,
            double pesoDistancia,
            double pesoAtraso,
            double pesoCongestionamento,
            double velocidadeMediaKmh,
            int tempoColetaSegundos,
            int tempoEntregaSegundos,
            int toleranciaTelemetriaSegundos,
            int capacidadeSimultanea,
            int limiteRegionalChe,
            boolean selecionarAuxiliar,
            boolean permitirOverride,
            LocalDateTime vigenteDe,
            LocalDateTime vigenteAte,
            String motivo,
            String criadoPor,
            LocalDateTime criadoEm,
            LocalDateTime ativadoEm) {
    }

    public record Rota(
            String origem,
            String destino,
            double distanciaMetros,
            double congestionamentoPercentual,
            int etaSegundos,
            boolean bloqueada,
            boolean telemetriaAtrasada,
            LocalDateTime telemetriaRecebidaEm,
            String justificativa) {
    }

    public record Ranking(
            int posicao,
            Long ordemTrabalhoPatioId,
            String codigoUnidade,
            String origem,
            String destino,
            double score,
            int etaSegundos,
            boolean elegivel,
            List<String> motivosBloqueio,
            String memoriaCalculo,
            Rota rota) {
    }

    public record Etapa(
            Long id,
            Long ordemTrabalhoPatioId,
            TipoEtapa tipo,
            int ordem,
            StatusEtapa status,
            LocalDateTime iniciadoEm,
            LocalDateTime concluidoEm,
            String operador,
            String evidencia,
            String chaveIdempotencia) {
    }

    public record Auxiliar(
            Long reservaId,
            Long unidadeInventarioId,
            String identificacao,
            String tipo,
            String status,
            String motivoSelecao) {
    }

    public record Decisao(
            Long id,
            String chaveIdempotencia,
            Long workQueueId,
            Long ordemTrabalhoPatioId,
            Long equipamentoPatioId,
            String equipamento,
            String tipoEquipamento,
            ModoDispatch modo,
            double score,
            int etaSegundos,
            String status,
            String memoriaCalculo,
            Rota rota,
            Auxiliar auxiliar,
            List<Etapa> etapas,
            LocalDateTime criadoEm) {
    }

    public record Resumo(
            long configuracoesAtivas,
            long decisoesUltimas24Horas,
            long instrucoesEmExecucao,
            long telemetriasAtrasadas,
            long auxiliaresReservados,
            List<Decisao> ultimasDecisoes) {
    }
}
