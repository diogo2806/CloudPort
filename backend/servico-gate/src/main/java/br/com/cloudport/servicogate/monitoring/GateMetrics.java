package br.com.cloudport.servicogate.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GateMetrics {

    private final MeterRegistry meterRegistry;

    public GateMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void registrarTempoValidacao(Duration duracao, boolean sucesso) {
        Timer timer = meterRegistry.timer("gate.validacao.tempo", "resultado", sucesso ? "sucesso" : "falha");
        timer.record(duracao);
    }

    public void registrarConsumoFila(String fila, boolean sucesso) {
        Counter counter = meterRegistry.counter("gate.hardware.mensagens.processadas",
                "fila", fila,
                "resultado", sucesso ? "sucesso" : "falha");
        counter.increment();
    }

    public void registrarEventoDegradacao(String sistema, String origem) {
        Counter counter = meterRegistry.counter("gate.integracoes.degradacao.total",
                "sistema", sistema,
                "origem", origem);
        counter.increment();
    }

    public void registrarContingenciaAcionada(String acao) {
        Counter counter = meterRegistry.counter("gate.contingencia.eventos",
                "acao", acao);
        counter.increment();
    }
}
