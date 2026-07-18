package br.com.cloudport.servicoyard.inventario.controlador;

import br.com.cloudport.servicoyard.inventario.dto.InventarioCanonicoDTO;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.servico.InventarioCanonicoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/inventario/canonico")
@Tag(name = "Inventário Canônico", description = "Gestão unificada de unidades e equipamentos do terminal")
public class InventarioCanonicoControlador {

    private final InventarioCanonicoServico inventarioServico;

    public InventarioCanonicoControlador(InventarioCanonicoServico inventarioServico) {
        this.inventarioServico = inventarioServico;
    }

    @GetMapping("/unidades")
    @Operation(summary = "Consultar inventário canônico")
    public InventarioCanonicoDTO.DashboardInventarioResposta listarUnidades(
            @RequestParam(value = "identificacao", required = false) String identificacao,
            @RequestParam(value = "categoria", required = false)
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            @RequestParam(value = "estado", required = false) UnidadeInventario.EstadoUnidade estado,
            @RequestParam(value = "condicao", required = false) UnidadeInventario.CondicaoEquipamento condicao,
            @RequestParam(value = "proprietario", required = false) String proprietario,
            @RequestParam(value = "operador", required = false) String operador,
            @RequestParam(value = "somenteComHold", required = false) Boolean somenteComHold,
            @RequestParam(value = "somenteReefer", required = false) Boolean somenteReefer) {
        return inventarioServico.listar(
                identificacao,
                categoria,
                estado,
                condicao,
                proprietario,
                operador,
                somenteComHold,
                somenteReefer);
    }

    @GetMapping("/unidades/{unidadeId}")
    @Operation(summary = "Abrir inspector completo da unidade")
    public InventarioCanonicoDTO.UnidadeDetalheResposta detalharUnidade(@PathVariable Long unidadeId) {
        return inventarioServico.detalhar(unidadeId);
    }

    @PostMapping("/unidades")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar unidade ou equipamento")
    public InventarioCanonicoDTO.UnidadeDetalheResposta criarUnidade(
            @RequestBody InventarioCanonicoDTO.CriarUnidadeRequest request) {
        return inventarioServico.criarUnidade(request);
    }

    @PatchMapping("/unidades/{unidadeId}/estado")
    @Operation(summary = "Avançar o ciclo de vida da unidade")
    public InventarioCanonicoDTO.UnidadeDetalheResposta atualizarEstado(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.AtualizarEstadoRequest request) {
        return inventarioServico.atualizarEstado(unidadeId, request);
    }

    @PatchMapping("/unidades/{unidadeId}/propriedade")
    @Operation(summary = "Atualizar ownership e operador")
    public InventarioCanonicoDTO.UnidadeDetalheResposta atualizarPropriedade(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.AtualizarPropriedadeRequest request) {
        return inventarioServico.atualizarPropriedade(unidadeId, request);
    }

    @PatchMapping("/unidades/{unidadeId}/posicao")
    @Operation(summary = "Atualizar posição real e planejada")
    public InventarioCanonicoDTO.UnidadeDetalheResposta atualizarPosicao(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.AtualizarPosicaoRequest request) {
        return inventarioServico.atualizarPosicao(unidadeId, request);
    }

    @PostMapping("/unidades/{unidadeId}/lacres")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Anexar lacre à unidade")
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarLacre(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.LacreRequest request) {
        return inventarioServico.adicionarLacre(unidadeId, request);
    }

    @PostMapping("/unidades/{unidadeId}/documentos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Anexar documento à unidade")
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarDocumento(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.DocumentoRequest request) {
        return inventarioServico.adicionarDocumento(unidadeId, request);
    }

    @PostMapping("/unidades/{unidadeId}/avarias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar avaria, componente e condição")
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarAvaria(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.AvariaRequest request) {
        return inventarioServico.adicionarAvaria(unidadeId, request);
    }

    @PostMapping("/unidades/{unidadeId}/holds-permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar hold ou permission")
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarHoldPermission(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.RestricaoRequest request) {
        return inventarioServico.adicionarRestricao(unidadeId, request);
    }

    @PostMapping("/unidades/{unidadeId}/manutencoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar manutenção ou reparo")
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarManutencao(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.ManutencaoRequest request) {
        return inventarioServico.adicionarManutencao(unidadeId, request);
    }

    @PostMapping("/unidades/{unidadeId}/reefer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar leitura e condição reefer")
    public InventarioCanonicoDTO.UnidadeDetalheResposta registrarReefer(
            @PathVariable Long unidadeId,
            @RequestBody InventarioCanonicoDTO.ReeferRequest request) {
        return inventarioServico.registrarReefer(unidadeId, request);
    }

    @GetMapping("/tipos")
    @Operation(summary = "Listar tipos e equivalências de equipamento")
    public List<InventarioCanonicoDTO.TipoEquipamentoResposta> listarTipos() {
        return inventarioServico.listarTipos();
    }

    @PostMapping("/tipos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar tipo e dimensões de equipamento")
    public InventarioCanonicoDTO.TipoEquipamentoResposta criarTipo(
            @RequestBody InventarioCanonicoDTO.CriarTipoEquipamentoRequest request) {
        return inventarioServico.criarTipo(request);
    }

    @GetMapping("/prefixos")
    @Operation(summary = "Listar prefixos de equipamentos")
    public List<InventarioCanonicoDTO.PrefixoResposta> listarPrefixos() {
        return inventarioServico.listarPrefixos();
    }

    @PostMapping("/prefixos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar prefixo e proprietário")
    public InventarioCanonicoDTO.PrefixoResposta criarPrefixo(
            @RequestBody InventarioCanonicoDTO.CriarPrefixoRequest request) {
        return inventarioServico.criarPrefixo(request);
    }

    @PostMapping("/montagens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Montar ou associar equipamentos")
    public InventarioCanonicoDTO.VinculoResposta montar(
            @RequestBody InventarioCanonicoDTO.MontagemRequest request) {
        return inventarioServico.montar(request);
    }

    @PostMapping("/montagens/{vinculoId}/desmontagem")
    @Operation(summary = "Desmontar equipamentos associados")
    public InventarioCanonicoDTO.VinculoResposta desmontar(
            @PathVariable Long vinculoId,
            @RequestBody(required = false) InventarioCanonicoDTO.DesmontagemRequest request) {
        return inventarioServico.desmontar(vinculoId, request);
    }

    @PostMapping("/contagens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar contagem de inventário físico")
    public InventarioCanonicoDTO.ContagemLoteResposta registrarContagem(
            @RequestBody InventarioCanonicoDTO.ContagemLoteRequest request) {
        return inventarioServico.registrarContagem(request);
    }

    @GetMapping("/divergencias")
    @Operation(summary = "Listar divergências físicas abertas")
    public List<InventarioCanonicoDTO.DivergenciaResposta> listarDivergencias() {
        return inventarioServico.listarDivergencias();
    }

    @PostMapping("/divergencias/{divergenciaId}/resolucao")
    @Operation(summary = "Resolver divergência de inventário")
    public InventarioCanonicoDTO.DivergenciaResposta resolverDivergencia(
            @PathVariable Long divergenciaId,
            @RequestBody InventarioCanonicoDTO.ResolverDivergenciaRequest request) {
        return inventarioServico.resolverDivergencia(divergenciaId, request);
    }
}
