package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.StatusPatioDto;
import br.com.cloudport.servicoyard.patio.enumeracao.StatusServicoPatioEnum;
import br.com.cloudport.servicoyard.patio.servico.StatusPatioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/yard/status")
@Tag(name = "Status do Pátio", description = "Operações relacionadas ao monitoramento do serviço de pátio")
public class StatusPatioControlador {

    private final StatusPatioServico statusPatioServico;

    public StatusPatioControlador(StatusPatioServico statusPatioServico) {
        this.statusPatioServico = statusPatioServico;
    }

    @GetMapping
    @Operation(summary = "Consultar status do serviço de pátio",
            description = "Retorna o status atualizado do serviço de pátio considerando dependências críticas")
    public StatusPatioDto verificarStatus() {
        return statusPatioServico.verificarDisponibilidade();
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Listar opções de status do serviço de pátio",
            description = "Fornece valores e descrições dos status disponíveis para popular componentes de seleção")
    public ResponseEntity<List<Map<String, String>>> listarOpcoesStatus() {
        List<Map<String, String>> opcoes = new ArrayList<>();
        for (StatusServicoPatioEnum status : StatusServicoPatioEnum.values()) {
            Map<String, String> registro = Map.of(
                    "value", status.name(),
                    "label", HtmlUtils.htmlEscape(status.getDescricao(), "UTF-8")
            );
            opcoes.add(registro);
        }
        return ResponseEntity.ok(opcoes);
    }
}
