package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ConhecimentoDetalhe;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ConhecimentoResumo;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarConhecimentoRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarItemRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarLoteRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarReferenciaRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.DashboardResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ItemResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.LoteDetalhe;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.LoteResumo;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ReferenciaResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarMovimentacaoRequest;
import br.com.cloudport.servicocargageral.servico.CargaGeralServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/carga-geral")
@Tag(name = "Carga geral e break-bulk", description = "Bill of Lading, cargo lots, referências e execução física da carga")
public class CargaGeralControlador {

    private static final String ENDPOINT_AVARIA_CANONICO = "/api/carga-geral/intermodal/avarias";

    private final CargaGeralServico cargaGeralServico;

    public CargaGeralControlador(CargaGeralServico cargaGeralServico) {
        this.cargaGeralServico = cargaGeralServico;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar visão consolidada da carga geral")
    public DashboardResposta obterDashboard() {
        return cargaGeralServico.obterDashboard();
    }

    @GetMapping("/conhecimentos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar Bills of Lading")
    public List<ConhecimentoResumo> listarConhecimentos() {
        return cargaGeralServico.listarConhecimentos();
    }

    @PostMapping("/conhecimentos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar Bill of Lading")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bill of Lading criado"),
        @ApiResponse(responseCode = "409", description = "Número de Bill of Lading já cadastrado")
    })
    public ResponseEntity<ConhecimentoDetalhe> criarConhecimento(@Valid @RequestBody CriarConhecimentoRequest request) {
        ConhecimentoDetalhe criado = cargaGeralServico.criarConhecimento(request);
        return ResponseEntity.created(URI.create("/api/carga-geral/conhecimentos/" + criado.id())).body(criado);
    }

    @GetMapping("/conhecimentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar Bill of Lading com itens e lotes")
    public ConhecimentoDetalhe obterConhecimento(@PathVariable UUID id) {
        return cargaGeralServico.obterConhecimento(id);
    }

    @PostMapping("/conhecimentos/{id}/itens")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Adicionar item ao Bill of Lading")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item criado"),
        @ApiResponse(responseCode = "409", description = "Sequência de item já cadastrada no Bill of Lading")
    })
    public ResponseEntity<ItemResposta> adicionarItem(
            @PathVariable UUID id,
            @Valid @RequestBody CriarItemRequest request) {
        ItemResposta criado = cargaGeralServico.adicionarItem(id, request);
        return ResponseEntity.created(URI.create("/api/carga-geral/itens/" + criado.id())).body(criado);
    }

    @PostMapping("/itens/{id}/lotes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR')")
    @Operation(summary = "Criar cargo lot para um item do conhecimento")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cargo lot criado"),
        @ApiResponse(responseCode = "409", description = "Código de cargo lot já cadastrado")
    })
    public ResponseEntity<LoteResumo> adicionarLote(
            @PathVariable UUID id,
            @Valid @RequestBody CriarLoteRequest request) {
        LoteResumo criado = cargaGeralServico.adicionarLote(id, request);
        return ResponseEntity.created(URI.create("/api/carga-geral/lotes/" + criado.id())).body(criado);
    }

    @GetMapping("/lotes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar cargo lots")
    public List<LoteResumo> listarLotes(
            @RequestParam(required = false) StatusLoteCarga status,
            @RequestParam(required = false) NaturezaCarga natureza) {
        return cargaGeralServico.listarLotes(status, natureza);
    }

    @GetMapping("/lotes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Consultar cargo lot e histórico de movimentações")
    public LoteDetalhe obterLote(@PathVariable UUID id) {
        return cargaGeralServico.obterLote(id);
    }

    @PostMapping("/lotes/{id}/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Registrar recebimento, carga, descarga, consolidação ou transferência parcial")
    public LoteDetalhe registrarMovimentacao(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarMovimentacaoRequest request) {
        return cargaGeralServico.registrarMovimentacao(id, request);
    }

    @Deprecated
    @PostMapping("/lotes/{id}/avarias")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'OPERADOR_GATE')")
    @Operation(summary = "Endpoint legado desativado; use o ciclo canônico de avarias")
    @ApiResponse(responseCode = "410", description = "Registro simplificado removido para preservar o saldo segregado")
    public LoteDetalhe registrarAvariaLegada(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarAvariaRequest request) {
        throw new ResponseStatusException(
                HttpStatus.GONE,
                "O registro simplificado de avaria foi desativado. Use " + ENDPOINT_AVARIA_CANONICO + ".");
    }

    @GetMapping("/referencias")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
    @Operation(summary = "Listar commodities, embalagens, produtos e códigos operacionais")
    public List<ReferenciaResposta> listarReferencias(
            @RequestParam(required = false) CategoriaReferenciaCarga categoria) {
        return cargaGeralServico.listarReferencias(categoria);
    }

    @PostMapping("/referencias")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Criar referência do domínio de carga")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Referência criada"),
        @ApiResponse(responseCode = "409", description = "Código já cadastrado na categoria informada")
    })
    public ResponseEntity<ReferenciaResposta> criarReferencia(@Valid @RequestBody CriarReferenciaRequest request) {
        ReferenciaResposta criada = cargaGeralServico.criarReferencia(request);
        return ResponseEntity.created(URI.create("/api/carga-geral/referencias/" + criada.id())).body(criada);
    }

    @PatchMapping("/referencias/{id}/status")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    @Operation(summary = "Ativar ou desativar referência")
    public ReferenciaResposta atualizarReferenciaAtiva(
            @PathVariable UUID id,
            @RequestParam boolean ativo) {
        return cargaGeralServico.atualizarReferenciaAtiva(id, ativo);
    }
}
