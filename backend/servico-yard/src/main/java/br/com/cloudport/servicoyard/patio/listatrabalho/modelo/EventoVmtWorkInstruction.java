package br.com.cloudport.servicoyard.patio.listatrabalho.modelo;

import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "evento_vmt_work_instruction")
public class EventoVmtWorkInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(name = "ordem_trabalho_patio_id", nullable = false)
    private Long ordemTrabalhoPatioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoVmt tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_esperado", nullable = false, length = 30)
    private StatusConfirmacaoVmt statusEsperado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_resultante", nullable = false, length = 30)
    private StatusConfirmacaoVmt statusResultante;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(name = "resultado", length = 1000)
    private String resultado;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acao_fisica", length = 30)
    private TipoAcaoFisicaPatio tipoAcaoFisica;

    @Column(name = "codigo_unidade_lido", length = 40)
    private String codigoUnidadeLido;

    @Column(name = "equipamento_patio_id")
    private Long equipamentoPatioId;

    @Column(name = "equipamento_identificador", length = 80)
    private String equipamentoIdentificador;

    @Column(name = "origem_fisica", length = 120)
    private String origem;

    @Column(name = "destino_fisico", length = 120)
    private String destino;

    @Column(name = "linha_origem")
    private Integer linhaOrigem;

    @Column(name = "coluna_origem")
    private Integer colunaOrigem;

    @Column(name = "camada_origem", length = 40)
    private String camadaOrigem;

    @Column(name = "linha_destino")
    private Integer linhaDestino;

    @Column(name = "coluna_destino")
    private Integer colunaDestino;

    @Column(name = "camada_destino", length = 40)
    private String camadaDestino;

    @Column(name = "sequencia_operacional")
    private Integer sequenciaOperacional;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;

    @PrePersist
    public void preencherProcessamento() {
        if (processadoEm == null) {
            processadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public TipoEventoVmt getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoVmt tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusConfirmacaoVmt getStatusEsperado() { return statusEsperado; }
    public void setStatusEsperado(StatusConfirmacaoVmt statusEsperado) { this.statusEsperado = statusEsperado; }
    public StatusConfirmacaoVmt getStatusResultante() { return statusResultante; }
    public void setStatusResultante(StatusConfirmacaoVmt statusResultante) { this.statusResultante = statusResultante; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(LocalDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
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
    public LocalDateTime getProcessadoEm() { return processadoEm; }
}
