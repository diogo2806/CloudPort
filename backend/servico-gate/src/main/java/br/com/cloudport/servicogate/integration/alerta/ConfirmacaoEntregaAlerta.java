package br.com.cloudport.servicogate.integration.alerta;

import java.time.LocalDateTime;

public class ConfirmacaoEntregaAlerta {

    private final String canal;
    private final String identificadorExterno;
    private final LocalDateTime confirmadoEm;

    public ConfirmacaoEntregaAlerta(String canal,
                                    String identificadorExterno,
                                    LocalDateTime confirmadoEm) {
        this.canal = canal;
        this.identificadorExterno = identificadorExterno;
        this.confirmadoEm = confirmadoEm;
    }

    public String getCanal() { return canal; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
}
