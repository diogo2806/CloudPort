package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.TipoAcaoFisicaPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class EventoVmtWorkInstructionRequest {

    @NotBlank
    @Size(max = 120)
    private String eventId;

    @NotNull
    private TipoEventoVmt tipoEvento;

    @NotNull
    private StatusConfirmacaoVmt statusEsperado;

    @NotNull
    private LocalDateTime timestamp;

    @Size(max = 1000)
    private String resultado;

    private String payload;

    @Size(max = 120)
    private String operador;

    @Size(max = 120)
    private String correlationId;

    @Size(max = 20)
    private String versaoContrato;

    private TipoAcaoFisicaPatio tipoAcaoFisica;

    @Size(max = 40)
    private String codigoUnidadeLido;

    private Long equipamentoPatioId;

    @Size(max = 80)
    private String equipamentoIdentificador;

    @Size(max = 120)
    private String origem;

    @Size(max = 120)
    private String destino;

    private Integer linhaOrigem;
    private Integer colunaOrigem;

    @Size(max = 40)
    private String camadaOrigem;

    private Integer linhaDestino;
    private Integer colunaDestino;

    @Size(max = 40)
    private String camadaDestino;

    private Integer sequenciaOperacional;

    @Size(max = 80)
    private String numeroLacre;

    @Size(max = 80)
    private String codigoAvaria;

    @Size(max = 500)
    private String descricaoAvaria;

    @Size(max = 500)
    private String evidenciaUrl;

    private Boolean reeferConectadoDesejado;
    private BigDecimal temperaturaReefer;

    @Size(max = 80)
    private String unidadeAlvoRehandle;

    private Boolean rehandleObrigatorio;
    private Integer sequenciaRehandle;

    @Size(max = 80)
    private String etapaAnterior;

    @Size(max = 80)
    private String etapaNova;

    @Size(max = 500)
    private String motivoAjuste;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public TipoEventoVmt getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoVmt tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusConfirmacaoVmt getStatusEsperado() { return statusEsperado; }
    public void setStatusEsperado(StatusConfirmacaoVmt statusEsperado) { this.statusEsperado = statusEsperado; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getVersaoContrato() { return versaoContrato; }
    public void setVersaoContrato(String versaoContrato) { this.versaoContrato = versaoContrato; }
    public TipoAcaoFisicaPatio getTipoAcaoFisica() { return tipoAcaoFisica; }
    public void setTipoAcaoFisica(TipoAcaoFisicaPatio tipoAcaoFisica) { this.tipoAcaoFisica = tipoAcaoFisica; }
    public String getCodigoUnidadeLido() { return codigoUnidadeLido; }
    public void setCodigoUnidadeLido(String codigoUnidadeLido) { this.codigoUnidadeLido = codigoUnidadeLido; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
    public String getEquipamentoIdentificador() { return equipamentoIdentificador; }
    public void setEquipamentoIdentificador(String equipamentoIdentificador) { this.equipamentoIdentificador = equipamentoIdentificador; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public Integer getLinhaOrigem() { return linhaOrigem; }
    public void setLinhaOrigem(Integer linhaOrigem) { this.linhaOrigem = linhaOrigem; }
    public Integer getColunaOrigem() { return colunaOrigem; }
    public void setColunaOrigem(Integer colunaOrigem) { this.colunaOrigem = colunaOrigem; }
    public String getCamadaOrigem() { return camadaOrigem; }
    public void setCamadaOrigem(String camadaOrigem) { this.camadaOrigem = camadaOrigem; }
    public Integer getLinhaDestino() { return linhaDestino; }
    public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
    public Integer getColunaDestino() { return colunaDestino; }
    public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
    public String getCamadaDestino() { return camadaDestino; }
    public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
    public Integer getSequenciaOperacional() { return sequenciaOperacional; }
    public void setSequenciaOperacional(Integer sequenciaOperacional) { this.sequenciaOperacional = sequenciaOperacional; }
    public String getNumeroLacre() { return numeroLacre; }
    public void setNumeroLacre(String numeroLacre) { this.numeroLacre = numeroLacre; }
    public String getCodigoAvaria() { return codigoAvaria; }
    public void setCodigoAvaria(String codigoAvaria) { this.codigoAvaria = codigoAvaria; }
    public String getDescricaoAvaria() { return descricaoAvaria; }
    public void setDescricaoAvaria(String descricaoAvaria) { this.descricaoAvaria = descricaoAvaria; }
    public String getEvidenciaUrl() { return evidenciaUrl; }
    public void setEvidenciaUrl(String evidenciaUrl) { this.evidenciaUrl = evidenciaUrl; }
    public Boolean getReeferConectadoDesejado() { return reeferConectadoDesejado; }
    public void setReeferConectadoDesejado(Boolean reeferConectadoDesejado) { this.reeferConectadoDesejado = reeferConectadoDesejado; }
    public BigDecimal getTemperaturaReefer() { return temperaturaReefer; }
    public void setTemperaturaReefer(BigDecimal temperaturaReefer) { this.temperaturaReefer = temperaturaReefer; }
    public String getUnidadeAlvoRehandle() { return unidadeAlvoRehandle; }
    public void setUnidadeAlvoRehandle(String unidadeAlvoRehandle) { this.unidadeAlvoRehandle = unidadeAlvoRehandle; }
    public Boolean getRehandleObrigatorio() { return rehandleObrigatorio; }
    public void setRehandleObrigatorio(Boolean rehandleObrigatorio) { this.rehandleObrigatorio = rehandleObrigatorio; }
    public Integer getSequenciaRehandle() { return sequenciaRehandle; }
    public void setSequenciaRehandle(Integer sequenciaRehandle) { this.sequenciaRehandle = sequenciaRehandle; }
    public String getEtapaAnterior() { return etapaAnterior; }
    public void setEtapaAnterior(String etapaAnterior) { this.etapaAnterior = etapaAnterior; }
    public String getEtapaNova() { return etapaNova; }
    public void setEtapaNova(String etapaNova) { this.etapaNova = etapaNova; }
    public String getMotivoAjuste() { return motivoAjuste; }
    public void setMotivoAjuste(String motivoAjuste) { this.motivoAjuste = motivoAjuste; }
}
