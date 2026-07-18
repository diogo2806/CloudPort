package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.EventoVmtWorkInstruction;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;

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
    private LocalDateTime processadoEm;
    private OrdemTrabalhoPatioRespostaDto instrucao;

    public static EventoVmtWorkInstructionRespostaDto deEntidades(EventoVmtWorkInstruction evento,
                                                                   OrdemTrabalhoPatioRespostaDto instrucao) {
        EventoVmtWorkInstructionRespostaDto dto = new EventoVmtWorkInstructionRespostaDto();
        dto.id = evento.getId();
        dto.eventId = evento.getEventId();
        dto.instructionId = evento.getOrdemTrabalhoPatioId();
        dto.tipoEvento = evento.getTipoEvento();
        dto.statusEsperado = evento.getStatusEsperado();
        dto.statusResultante = evento.getStatusResultante();
        dto.timestamp = evento.getOcorridoEm();
        dto.resultado = evento.getResultado();
        dto.payload = evento.getPayload();
        dto.processadoEm = evento.getProcessadoEm();
        dto.instrucao = instrucao;
        return dto;
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
    public LocalDateTime getProcessadoEm() { return processadoEm; }
    public OrdemTrabalhoPatioRespostaDto getInstrucao() { return instrucao; }
}
