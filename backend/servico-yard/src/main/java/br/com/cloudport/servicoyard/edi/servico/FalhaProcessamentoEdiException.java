package br.com.cloudport.servicoyard.edi.servico;

public class FalhaProcessamentoEdiException extends RuntimeException {

    private final Long processamentoId;
    private final boolean irrecuperavel;

    public FalhaProcessamentoEdiException(Long processamentoId,
                                           boolean irrecuperavel,
                                           RuntimeException causa) {
        super(causa.getMessage(), causa);
        this.processamentoId = processamentoId;
        this.irrecuperavel = irrecuperavel;
    }

    public Long getProcessamentoId() {
        return processamentoId;
    }

    public boolean isIrrecuperavel() {
        return irrecuperavel;
    }
}
