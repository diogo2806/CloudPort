package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ComandoCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ConfigurarCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ReservaCapacidadeResposta;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.ReservarCapacidadeRequest;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.SaldoPosicaoResposta;
import br.com.cloudport.servicoyard.inventario.servico.CapacidadeCargoLotServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/capacidades-cargo-lot")
@Tag(name = "Capacidade de cargo lot", description = "Reserva de pátio e armazém para carga geral")
public class CapacidadeCargoLotControlador {

    private final CapacidadeCargoLotServico servico;

    public CapacidadeCargoLotControlador(CapacidadeCargoLotServico servico) {
        this.servico = servico;
    }

    @PutMapping("/{posicao}")
    @Operation(summary = "Configurar capacidade e restrições da posição")
    public ResponseEntity<Void> configurar(
            @PathVariable String posicao,
            @Valid @RequestBody ConfigurarCapacidadeRequest request) {
        servico.configurar(posicao, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{posicao}/saldos")
    @Operation(summary = "Listar saldos confirmados de cargo lots na posição")
    public List<SaldoPosicaoResposta> listarSaldos(@PathVariable String posicao) {
        return servico.listarSaldos(posicao);
    }

    @PostMapping("/{posicao}/reservas")
    @Operation(summary = "Reservar capacidade para um cargo lot")
    public ReservaCapacidadeResposta reservar(
            @PathVariable String posicao,
            @Valid @RequestBody ReservarCapacidadeRequest request) {
        return servico.reservar(posicao, request);
    }

    @PostMapping("/reservas/{reservaId}/confirmar")
    @Operation(summary = "Confirmar ocupação após execução física")
    public ReservaCapacidadeResposta confirmar(
            @PathVariable UUID reservaId,
            @Valid @RequestBody ComandoCapacidadeRequest request) {
        return servico.confirmar(reservaId, request);
    }

    @PostMapping("/reservas/{reservaId}/cancelar")
    @Operation(summary = "Cancelar reserva de capacidade")
    public ReservaCapacidadeResposta cancelar(
            @PathVariable UUID reservaId,
            @Valid @RequestBody ComandoCapacidadeRequest request) {
        return servico.cancelar(reservaId, request);
    }
}
