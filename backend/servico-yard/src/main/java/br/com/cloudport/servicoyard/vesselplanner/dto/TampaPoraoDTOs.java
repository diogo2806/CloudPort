package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.EstadoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.StatusTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoPosicaoTampaPorao;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class TampaPoraoDTOs {

    private TampaPoraoDTOs() {
    }

    public static class CriarTarefaRequest {

        @NotNull
        private TipoOperacaoTampaPorao tipo;

        @NotBlank
        @Size(max = 120)
        private String recurso;

        @Size(max = 500)
        private String motivo;

        private TipoPosicaoTampaPorao posicaoDestinoTipo;

        @Size(max = 120)
        private String posicaoDestinoReferencia;

        private List<Long> dependenciasIds = new ArrayList<>();

        public TipoOperacaoTampaPorao getTipo() { return tipo; }
        public void setTipo(TipoOperacaoTampaPorao tipo) { this.tipo = tipo; }
        public String getRecurso() { return recurso; }
        public void setRecurso(String recurso) { this.recurso = recurso; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public TipoPosicaoTampaPorao getPosicaoDestinoTipo() { return posicaoDestinoTipo; }
        public void setPosicaoDestinoTipo(TipoPosicaoTampaPorao posicaoDestinoTipo) { this.posicaoDestinoTipo = posicaoDestinoTipo; }
        public String getPosicaoDestinoReferencia() { return posicaoDestinoReferencia; }
        public void setPosicaoDestinoReferencia(String posicaoDestinoReferencia) { this.posicaoDestinoReferencia = posicaoDestinoReferencia; }
        public List<Long> getDependenciasIds() { return dependenciasIds; }
        public void setDependenciasIds(List<Long> dependenciasIds) {
            this.dependenciasIds = dependenciasIds == null ? new ArrayList<>() : dependenciasIds;
        }
    }

    public static class ComandoTarefaRequest {

        @Size(max = 500)
        private String motivo;

        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }

    public static class TampaResposta {

        private Long id;
        private String codigo;
        private EstadoTampaPorao estado;
        private Long versao;
        private boolean tarefaEmExecucao;
        private PosicaoResposta posicaoAtual;
        private List<PosicaoResposta> posicoes = new ArrayList<>();
        private List<TarefaResposta> tarefas = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public EstadoTampaPorao getEstado() { return estado; }
        public void setEstado(EstadoTampaPorao estado) { this.estado = estado; }
        public Long getVersao() { return versao; }
        public void setVersao(Long versao) { this.versao = versao; }
        public boolean isTarefaEmExecucao() { return tarefaEmExecucao; }
        public void setTarefaEmExecucao(boolean tarefaEmExecucao) { this.tarefaEmExecucao = tarefaEmExecucao; }
        public PosicaoResposta getPosicaoAtual() { return posicaoAtual; }
        public void setPosicaoAtual(PosicaoResposta posicaoAtual) { this.posicaoAtual = posicaoAtual; }
        public List<PosicaoResposta> getPosicoes() { return posicoes; }
        public void setPosicoes(List<PosicaoResposta> posicoes) { this.posicoes = posicoes; }
        public List<TarefaResposta> getTarefas() { return tarefas; }
        public void setTarefas(List<TarefaResposta> tarefas) { this.tarefas = tarefas; }
    }

    public static class PosicaoResposta {

        private Long id;
        private TipoPosicaoTampaPorao tipo;
        private String referencia;
        private boolean ativa;
        private LocalDateTime inicioEm;
        private LocalDateTime fimEm;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public TipoPosicaoTampaPorao getTipo() { return tipo; }
        public void setTipo(TipoPosicaoTampaPorao tipo) { this.tipo = tipo; }
        public String getReferencia() { return referencia; }
        public void setReferencia(String referencia) { this.referencia = referencia; }
        public boolean isAtiva() { return ativa; }
        public void setAtiva(boolean ativa) { this.ativa = ativa; }
        public LocalDateTime getInicioEm() { return inicioEm; }
        public void setInicioEm(LocalDateTime inicioEm) { this.inicioEm = inicioEm; }
        public LocalDateTime getFimEm() { return fimEm; }
        public void setFimEm(LocalDateTime fimEm) { this.fimEm = fimEm; }
    }

    public static class TarefaResposta {

        private Long id;
        private TipoOperacaoTampaPorao tipo;
        private StatusTarefaTampaPorao status;
        private String recurso;
        private String operador;
        private String motivo;
        private TipoPosicaoTampaPorao posicaoDestinoTipo;
        private String posicaoDestinoReferencia;
        private List<Long> dependenciasIds = new ArrayList<>();
        private LocalDateTime criadoEm;
        private LocalDateTime iniciadoEm;
        private LocalDateTime concluidoEm;
        private LocalDateTime canceladoEm;
        private Long versao;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public TipoOperacaoTampaPorao getTipo() { return tipo; }
        public void setTipo(TipoOperacaoTampaPorao tipo) { this.tipo = tipo; }
        public StatusTarefaTampaPorao getStatus() { return status; }
        public void setStatus(StatusTarefaTampaPorao status) { this.status = status; }
        public String getRecurso() { return recurso; }
        public void setRecurso(String recurso) { this.recurso = recurso; }
        public String getOperador() { return operador; }
        public void setOperador(String operador) { this.operador = operador; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public TipoPosicaoTampaPorao getPosicaoDestinoTipo() { return posicaoDestinoTipo; }
        public void setPosicaoDestinoTipo(TipoPosicaoTampaPorao posicaoDestinoTipo) { this.posicaoDestinoTipo = posicaoDestinoTipo; }
        public String getPosicaoDestinoReferencia() { return posicaoDestinoReferencia; }
        public void setPosicaoDestinoReferencia(String posicaoDestinoReferencia) { this.posicaoDestinoReferencia = posicaoDestinoReferencia; }
        public List<Long> getDependenciasIds() { return dependenciasIds; }
        public void setDependenciasIds(List<Long> dependenciasIds) { this.dependenciasIds = dependenciasIds; }
        public LocalDateTime getCriadoEm() { return criadoEm; }
        public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
        public LocalDateTime getIniciadoEm() { return iniciadoEm; }
        public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }
        public LocalDateTime getConcluidoEm() { return concluidoEm; }
        public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
        public LocalDateTime getCanceladoEm() { return canceladoEm; }
        public void setCanceladoEm(LocalDateTime canceladoEm) { this.canceladoEm = canceladoEm; }
        public Long getVersao() { return versao; }
        public void setVersao(Long versao) { this.versao = versao; }
    }
}
