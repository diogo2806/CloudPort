package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dominio.StatusOrdemFerroviariaCarga;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.AtualizarStatusOrdemFerroviariaRequest;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.OrdemFerroviariaCargaResposta;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.PlanejarOrdemFerroviariaRequest;
import br.com.cloudport.servicocargageral.servico.OrdemMovimentacaoFerroviariaCargaGeralServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/ferrovia/lista-trabalho")
@Tag(name = "Lista de trabalho ferroviária de carga geral")
public class ListaTrabalhoTremCargaGeralControlador {

    private final OrdemMovimentacaoFerroviariaCargaGeralServico servico;

    public ListaTrabalhoTremCargaGeralControlador(
            OrdemMovimentacaoFerroviariaCargaGeralServico servico) {
        this.servico = servico;
    }

    @PostMapping("/visitas/{visitaTremId}/ordens")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Planejar cargo lot em vagão da visita ferroviária")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ordem ferroviária planejada"),
        @ApiResponse(responseCode = "409", description = "Peso excede a capacidade do vagão")
    })
    public ResponseEntity<OrdemFerroviariaCargaResposta> planejar(
            @PathVariable String visitaTremId,
            @Valid @RequestBody PlanejarOrdemFerroviariaRequest request) {
        OrdemFerroviariaCargaResposta resposta = servico.planejar(visitaTremId, request);
        URI localizacao = URI.create(
                "/api/carga-geral/ferrovia/lista-trabalho/visitas/"
                        + visitaTremId
                        + "/ordens/"
                        + resposta.loteId());
        return ResponseEntity.created(localizacao).body(resposta);
    }

    @GetMapping("/visitas/{visitaTremId}/ordens")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar ordens ferroviárias de carga geral por visita")
    public List<OrdemFerroviariaCargaResposta> listar(
            @PathVariable String visitaTremId,
            @RequestParam(required = false) StatusOrdemFerroviariaCarga status) {
        return servico.listar(visitaTremId, status);
    }

    @PatchMapping("/visitas/{visitaTremId}/ordens/{loteId}/status")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Atualizar status e custódia da ordem ferroviária")
    public OrdemFerroviariaCargaResposta atualizarStatus(
            @PathVariable String visitaTremId,
            @PathVariable UUID loteId,
            @Valid @RequestBody AtualizarStatusOrdemFerroviariaRequest request) {
        return servico.atualizarStatus(visitaTremId, loteId, request);
    }
}
