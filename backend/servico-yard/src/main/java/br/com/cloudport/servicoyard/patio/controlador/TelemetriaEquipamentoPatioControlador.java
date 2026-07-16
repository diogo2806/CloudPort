package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.TelemetriaEquipamentoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.TelemetriaEquipamentoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.servico.TelemetriaEquipamentoPatioServico;
import br.com.cloudport.servicoyard.patio.servico.TelemetriaEquipamentoStreamingServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/yard/patio/equipamentos/telemetria")
public class TelemetriaEquipamentoPatioControlador {

    private final TelemetriaEquipamentoPatioServico telemetriaServico;
    private final TelemetriaEquipamentoStreamingServico streamingServico;

    public TelemetriaEquipamentoPatioControlador(
            TelemetriaEquipamentoPatioServico telemetriaServico,
            TelemetriaEquipamentoStreamingServico streamingServico
    ) {
        this.telemetriaServico = telemetriaServico;
        this.streamingServico = streamingServico;
    }

    @GetMapping
    public List<TelemetriaEquipamentoPatioDto> listar() {
        return telemetriaServico.listar();
    }

    @GetMapping("/{identificador}")
    public TelemetriaEquipamentoPatioDto detalhar(@PathVariable String identificador) {
        return telemetriaServico.detalhar(identificador);
    }

    @PostMapping("/{identificador}")
    public TelemetriaEquipamentoPatioDto registrar(
            @PathVariable String identificador,
            @Valid @RequestBody TelemetriaEquipamentoPatioRequisicaoDto requisicao
    ) {
        return telemetriaServico.registrar(identificador, requisicao);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(name = "Last-Event-ID", required = false) String ultimoEventoId
    ) {
        return streamingServico.assinar(ultimoEventoId, telemetriaServico.listar());
    }
}
