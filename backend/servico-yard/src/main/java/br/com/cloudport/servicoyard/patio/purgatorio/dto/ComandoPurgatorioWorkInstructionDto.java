package br.com.cloudport.servicoyard.patio.purgatorio.dto;

import br.com.cloudport.servicoyard.patio.purgatorio.modelo.CausaPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.SeveridadePurgatorioWorkInstruction;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ComandoPurgatorioWorkInstructionDto {

    private Long ordemTrabalhoPatioId;
    private Long workQueueId;
    private CausaPurgatorioWorkInstruction causa;
    private SeveridadePurgatorioWorkInstruction severidade;

    @NotBlank
    private String chaveIdempotencia;

    @NotBlank
    private String motivo;

    private String usuario;
    private String origem;
    private String correlationId;
    private String snapshotOriginal;
    private String snapshotAtual;
    private String evidencias;
    private String resolucao;
    private Boolean revalidacaoBemSucedida;

    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public CausaPurgatorioWorkInstruction getCausa() { return causa; }
    public void setCausa(CausaPurgatorioWorkInstruction causa) { this.causa = causa; }
    public SeveridadePurgatorioWorkInstruction getSeveridade() { return severidade; }
    public void setSeveridade(SeveridadePurgatorioWorkInstruction severidade) { this.severidade = severidade; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getSnapshotOriginal() { return snapshotOriginal; }
    public void setSnapshotOriginal(String snapshotOriginal) { this.snapshotOriginal = snapshotOriginal; }
    public String getSnapshotAtual() { return snapshotAtual; }
    public void setSnapshotAtual(String snapshotAtual) { this.snapshotAtual = snapshotAtual; }
    public String getEvidencias() { return evidencias; }
    public void setEvidencias(String evidencias) { this.evidencias = evidencias; }
    public String getResolucao() { return resolucao; }
    public void setResolucao(String resolucao) { this.resolucao = resolucao; }
    public Boolean getRevalidacaoBemSucedida() { return revalidacaoBemSucedida; }
    public void setRevalidacaoBemSucedida(Boolean revalidacaoBemSucedida) { this.revalidacaoBemSucedida = revalidacaoBemSucedida; }
}
