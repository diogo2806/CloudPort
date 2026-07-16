package br.com.cloudport.monolitonavio.erro;

import java.time.OffsetDateTime;
import java.util.List;

public class ErroApi {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String codigo;
    private final String mensagem;
    private final String caminho;
    private final String correlationId;
    private final List<String> detalhes;

    public ErroApi(OffsetDateTime timestamp,
                   int status,
                   String codigo,
                   String mensagem,
                   String caminho,
                   String correlationId,
                   List<String> detalhes) {
        this.timestamp = timestamp;
        this.status = status;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.caminho = caminho;
        this.correlationId = correlationId;
        this.detalhes = detalhes;
    }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getCodigo() { return codigo; }
    public String getMensagem() { return mensagem; }
    public String getCaminho() { return caminho; }
    public String getCorrelationId() { return correlationId; }
    public List<String> getDetalhes() { return detalhes; }
}
