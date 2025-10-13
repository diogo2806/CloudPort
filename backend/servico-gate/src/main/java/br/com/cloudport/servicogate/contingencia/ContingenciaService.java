package br.com.cloudport.servicogate.contingencia;

import br.com.cloudport.servicogate.contingencia.dto.ContingenciaAgendamentoRequest;
import br.com.cloudport.servicogate.contingencia.dto.ContingenciaLiberacaoRequest;
import br.com.cloudport.servicogate.contingencia.dto.ContingenciaResponse;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContingenciaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContingenciaService.class);

    private final ContingenciaProperties properties;
    private final GateMetrics gateMetrics;

    public ContingenciaService(ContingenciaProperties properties, GateMetrics gateMetrics) {
        this.properties = properties;
        this.gateMetrics = gateMetrics;
    }

    public ContingenciaResponse agendar(ContingenciaAgendamentoRequest request) {
        String protocolo = gerarProtocolo();
        gateMetrics.registrarContingenciaAcionada("agendar");
        LOGGER.warn("event=contingencia.agendar codigo={} operador={} motivo=\"{}\" protocolo={} orientacao=\"{}\"",
                request.getCodigo(), request.getOperador(), request.getMotivo(), protocolo, properties.getOrientacaoOperador());
        String orientacoes = String.format("%s | Registrar atendimento manual para o código %s (motivo: %s).",
                properties.getOrientacaoOperador(), request.getCodigo(), request.getMotivo());
        return new ContingenciaResponse(protocolo, orientacoes);
    }

    public ContingenciaResponse liberar(ContingenciaLiberacaoRequest request) {
        String protocolo = gerarProtocolo();
        gateMetrics.registrarContingenciaAcionada("liberar");
        LOGGER.warn("event=contingencia.liberar codigo={} operador={} observacao=\"{}\" protocolo={} orientacao=\"{}\"",
                request.getCodigo(), request.getOperador(), request.getObservacao(), protocolo, properties.getOrientacaoOperador());
        String orientacoes = String.format("%s | Autorizar manualmente o gate para o código %s. Observação: %s.",
                properties.getOrientacaoOperador(), request.getCodigo(),
                request.getObservacao() != null ? request.getObservacao() : "sem observação");
        return new ContingenciaResponse(protocolo, orientacoes);
    }

    private String gerarProtocolo() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        return timestamp + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
