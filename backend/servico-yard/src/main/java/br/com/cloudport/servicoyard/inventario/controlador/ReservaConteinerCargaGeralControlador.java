package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.ConteinerInventarioResposta;
import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.LiberarConteinerRequest;
import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.ReservarConteinerRequest;
import br.com.cloudport.servicoyard.inventario.servico.ReservaConteinerCargaGeralServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/inventario/canonico/reservas-carga-geral")
@Tag(name = "Reservas de contêiner", description = "Reserva canônica para operações de stuff e unstuff")
public class ReservaConteinerCargaGeralControlador {

    private final ReservaConteinerCargaGeralServico servico;

    public ReservaConteinerCargaGeralControlador(ReservaConteinerCargaGeralServico servico) {
        this.servico = servico;
    }

    @GetMapping("/elegiveis")
    @Operation(summary = "Listar contêineres elegíveis para carga geral")
    public List<ConteinerInventarioResposta> listarElegiveis() {
        return servico.listarElegiveis();
    }

    @PostMapping("/{identificacao}")
    @Operation(summary = "Reservar contêiner para uma operação de stuff ou unstuff")
    public ConteinerInventarioResposta reservar(
            @PathVariable String identificacao,
            @Valid @RequestBody ReservarConteinerRequest request) {
        return servico.reservar(identificacao, request);
    }

    @PostMapping("/{operacaoId}/liberar")
    @Operation(summary = "Liberar reserva ao concluir ou cancelar a operação")
    public ConteinerInventarioResposta liberar(
            @PathVariable UUID operacaoId,
            @Valid @RequestBody LiberarConteinerRequest request) {
        return servico.liberar(operacaoId, request);
    }
}
