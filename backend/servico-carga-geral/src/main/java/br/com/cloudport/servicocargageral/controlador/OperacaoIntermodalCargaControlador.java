package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.AbrirInventarioRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.AtribuirRecursosRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.AvariaResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.CancelarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ComandoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ConciliarInventarioRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ConcluirPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.CriarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.EncerrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ExecutarItemPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.InspecionarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.InventarioResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.NovaVersaoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.PlanoResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.RegistrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.RegistrarContagemRequest;
import br.com.cloudport.servicocargageral.servico.AvariaInventarioCargaServico;
import br.com.cloudport.servicocargageral.servico.PlanoOperacionalCargaServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carga-geral/intermodal")
@Tag(name = "Operações intermodais de carga geral")
public class OperacaoIntermodalCargaControlador {

    private final PlanoOperacionalCargaServico planoServico;
    private final AvariaInventarioCargaServico avariaInventarioServico;

    public OperacaoIntermodalCargaControlador(
            PlanoOperacionalCargaServico planoServico,
            AvariaInventarioCargaServico avariaInventarioServico) {
        this.planoServico = planoServico;
        this.avariaInventarioServico = avariaInventarioServico;
    }

    @GetMapping("/planos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','OPERADOR_PATIO')")
    @Operation(summary = "Listar planos intermodais")
    public List<PlanoResposta> listarPlanos() { return planoServico.listar(); }

    @PostMapping("/planos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    @Operation(summary = "Criar plano sem movimentar estoque")
    public ResponseEntity<PlanoResposta> criarPlano(@Valid @RequestBody CriarPlanoRequest request) {
        PlanoResposta resposta = planoServico.criar(request);
        return ResponseEntity.created(URI.create("/api/carga-geral/intermodal/planos/" + resposta.id())).body(resposta);
    }

    @GetMapping("/planos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','OPERADOR_PATIO')")
    public PlanoResposta obterPlano(@PathVariable UUID id) { return planoServico.obter(id); }

    @PostMapping("/planos/{id}/versoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public PlanoResposta criarVersao(@PathVariable UUID id, @Valid @RequestBody NovaVersaoPlanoRequest request) {
        return planoServico.criarNovaVersao(id, request);
    }

    @PostMapping("/planos/{id}/liberar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public PlanoResposta liberarPlano(@PathVariable UUID id, @Valid @RequestBody ComandoPlanoRequest request) {
        return planoServico.liberar(id, request);
    }

    @PostMapping("/planos/{id}/recursos")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public PlanoResposta atribuirRecursos(@PathVariable UUID id, @Valid @RequestBody AtribuirRecursosRequest request) {
        return planoServico.atribuirRecursos(id, request);
    }

    @PostMapping("/planos/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','OPERADOR_PATIO')")
    public PlanoResposta iniciarPlano(@PathVariable UUID id, @Valid @RequestBody ComandoPlanoRequest request) {
        return planoServico.iniciar(id, request);
    }

    @PostMapping("/planos/{id}/execucoes")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','OPERADOR_PATIO')")
    @Operation(summary = "Registrar execução parcial idempotente")
    public PlanoResposta executarPlano(@PathVariable UUID id, @Valid @RequestBody ExecutarItemPlanoRequest request) {
        return planoServico.executar(id, request);
    }

    @PostMapping("/planos/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','OPERADOR_PATIO')")
    public PlanoResposta concluirPlano(@PathVariable UUID id, @Valid @RequestBody ConcluirPlanoRequest request) {
        return planoServico.concluir(id, request);
    }

    @PostMapping("/planos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public PlanoResposta cancelarPlano(@PathVariable UUID id, @Valid @RequestBody CancelarPlanoRequest request) {
        return planoServico.cancelar(id, request);
    }

    @GetMapping("/lotes/{loteId}/avarias")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE','OPERADOR_PATIO')")
    public List<AvariaResposta> listarAvarias(@PathVariable UUID loteId) {
        return avariaInventarioServico.listarAvarias(loteId);
    }

    @PostMapping("/avarias")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','OPERADOR_PATIO')")
    public AvariaResposta registrarAvaria(@Valid @RequestBody RegistrarAvariaRequest request) {
        return avariaInventarioServico.registrarAvaria(request);
    }

    @PostMapping("/avarias/{id}/inspecionar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
    public AvariaResposta inspecionarAvaria(@PathVariable UUID id,
            @Valid @RequestBody InspecionarAvariaRequest request) {
        return avariaInventarioServico.inspecionar(id, request);
    }

    @PostMapping("/avarias/{id}/encerrar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public AvariaResposta encerrarAvaria(@PathVariable UUID id,
            @Valid @RequestBody EncerrarAvariaRequest request) {
        return avariaInventarioServico.encerrar(id, request);
    }

    @GetMapping("/inventarios")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
    public List<InventarioResposta> listarInventarios() { return avariaInventarioServico.listarInventarios(); }

    @PostMapping("/inventarios")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public InventarioResposta abrirInventario(@Valid @RequestBody AbrirInventarioRequest request) {
        return avariaInventarioServico.abrirInventario(request);
    }

    @GetMapping("/inventarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')")
    public InventarioResposta obterInventario(@PathVariable UUID id) {
        return avariaInventarioServico.obterInventario(id);
    }

    @PostMapping("/inventarios/{id}/contagens")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO')")
    public InventarioResposta registrarContagem(@PathVariable UUID id,
            @Valid @RequestBody RegistrarContagemRequest request) {
        return avariaInventarioServico.registrarContagem(id, request);
    }

    @PostMapping("/inventarios/{id}/enviar-aprovacao")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO')")
    public InventarioResposta enviarAprovacao(@PathVariable UUID id,
            @RequestParam(defaultValue = "operador") String usuario) {
        return avariaInventarioServico.enviarParaAprovacao(id, usuario);
    }

    @PostMapping("/inventarios/{id}/conciliar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public InventarioResposta conciliarInventario(@PathVariable UUID id,
            @Valid @RequestBody ConciliarInventarioRequest request) {
        return avariaInventarioServico.conciliar(id, request);
    }
}
