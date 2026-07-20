package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.EventoVmtWorkInstruction;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.TipoAcaoFisicaPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import org.springframework.web.util.HtmlUtils;

public class EventoVmtWorkInstructionRespostaDto {

    private Long id;
    private String eventId;
    private Long instructionId;
    private TipoEventoVmt tipoEvento;
    private StatusConfirmacaoVmt statusEsperado;
    private StatusConfirmacaoVmt statusResultante;
    private LocalDateTime timestamp;
    private String resultado;
    private String payload;
    private TipoAcaoFisicaPatio tipoAcaoFisica;
    private String codigoUnidadeLido;
    private Long equipamentoPatioId;
    private String equipamentoIdentificador;
    private String origem;
    private String destino;
    private Integer linhaOrigem;
    private Integer colunaOrigem;
    private String camadaOrigem;
    private Integer linhaDestino;
    private Integer colunaDestino;
    private String camadaDestino;
    private Integer sequenciaOperacional;
    private LocalDateTime processadoEm;
    private OrdemTrabalhoPatioRespostaDto instrucao;

    public static EventoVmtWorkInstructionRespostaDto deEntidades(EventoVmtWorkInstruction evento,
                                                                    OrdemTrabalhoPatioRespostaDto instrucao) {
        EventoVmtWorkInstructionRespostaDto dto = new EventoVmtWorkInstructionRespostaDto();
        dto.id = evento.getId();
        dto.eventId = escapar(evento.getEventId());
        dto.instructionId = evento.getOrdemTrabalhoPatioId();
        dto.tipoEvento = evento.getTipoEvento();
        dto.statusEsperado = evento.getStatusEsperado();
        dto.statusResultante = evento.getStatusResultante();
        dto.timestamp = evento.getOcorridoEm();
        dto.resultado = escapar(evento.getResultado());
        dto.payload = escapar(evento.getPayload());
        dto.tipoAcaoFisica = evento.getTipoAcaoFisica();
        dto.codigoUnidadeLido = escapar(evento.getCodigoUnidadeLido());
        dto.equipamentoPatioId = evento.getEquipamentoPatioId();
        dto.equipamentoIdentificador = escapar(evento.getEquipamentoIdentificador());
        dto.origem = escapar(evento.getOrigem());
        dto.destino = escapar(evento.getDestino());
        dto.linhaOrigem = evento.getLinhaOrigem();
        dto.colunaOrigem = evento.getColunaOrigem();
        dto.camadaOrigem = escapar(evento.getCamadaOrigem());
        dto.linhaDestino = evento.getLinhaDestino();
        dto.colunaDestino = evento.getColunaDestino();
        dto.camadaDestino = escapar(evento.getCamadaDestino());
        dto.sequenciaOperacional = evento.getSequenciaOperacional();
        dto.processadoEm = evento.getProcessadoEm();
        dto.instrucao = instrucao;
        return dto;
    }

    private static String escapar(String valor) {
        return valor == null ? null : HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getInstructionId() { return instructionId; }
    public TipoEventoVmt getTipoEvento() { return tipoEvento; }
    public StatusConfirmacaoVmt getStatusEsperado() { return statusEsperado; }
    public StatusConfirmacaoVmt getStatusResultante() { return statusResultante; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getResultado() { return resultado; }
    public String getPayload() { return payload; }
    public TipoAcaoFisicaPatio getTipoAcaoFisica() { return tipoAcaoFisica; }
    public String getCodigoUnidadeLido() { return codigoUnidadeLido; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public String getEquipamentoIdentificador() { return equipamentoIdentificador; }
    public String getOrigem() { return origem; }
    public String getDestino() { return destino; }
    public Integer getLinhaOrigem() { return linhaOrigem; }
    public Integer getColunaOrigem() { return colunaOrigem; }
    public String getCamadaOrigem() { return camadaOrigem; }
    public Integer getLinhaDestino() { return linhaDestino; }
    public Integer getColunaDestino() { return colunaDestino; }
    public String getCamadaDestino() { return camadaDestino; }
    public Integer getSequenciaOperacional() { return sequenciaOperacional; }
    public LocalDateTime getProcessadoEm() { return processadoEm; }
    public OrdemTrabalhoPatioRespostaDto getInstrucao() { return instrucao; }
}
