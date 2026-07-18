package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.ModalTransporteCargo;
import br.com.cloudport.servicocargageral.dto.IdentificacaoCargoLotDTOs.IdentificacaoResposta;
import br.com.cloudport.servicocargageral.dto.IdentificacaoCargoLotDTOs.RegistrarIdentificacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AdicionarEvidenciaAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AbrirAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AbrirInventarioFisicoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AlocacaoResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AvariaResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ComandoMotivadoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.CompensarGateCargaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ConfirmarGateCargaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.CriarAlocacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ExecutarTransporteRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.InventarioFisicoResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.PlanejarTransporteRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.PlanoTransporteResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.RegistrarContagemRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ReservaGateCargaResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ReservarGateCargaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ResolverDivergenciaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.TransicionarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.TransloadResposta;
import br.com.cloudport.servicocargageral.servico.IdentificacaoCargoLotServico;
import br.com.cloudport.servicocargageral.servico.OperacoesIntermodaisServico;
import br.com.cloudport.servicocargageral.servico.TransloadServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/carga-geral/operacoes-intermodais")
@Tag(name = "Operações intermodais de carga geral")
@PreAuthorize("hasAnyRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')")
public class OperacoesIntermodaisControlador {

    private final OperacoesIntermodaisServico servico;
    private final IdentificacaoCargoLotServico identificacaoServico;
    private final TransloadServico transloadServico;

    public OperacoesIntermodaisControlador(
            OperacoesIntermodaisServico servico,
            IdentificacaoCargoLotServico identificacaoServico,
            TransloadServico transloadServico) {
        this.servico = servico;
        this.identificacaoServico = identificacaoServico;
        this.transloadServico = transloadServico;
    }

    @PostMapping("/transloads")
    @Operation(summary = "Executar transload atômico e recuperável entre unidades")
    public TransloadResposta executarTransload(@Valid @RequestBody ExecutarTransloadRequest request) {
        return transloadServico.executarTransload(request);
    }

    @GetMapping("/transloads/{id}")
    @Operation(summary = "Consultar rastreabilidade de uma operação de transload")
    public TransloadResposta obterTransload(@PathVariable UUID id) {
        return transloadServico.obter(id);
    }

    @PostMapping("/gate/reservas")
    @Operation(summary = "Reservar quantidade de cargo lot para o Gate")
    public ReservaGateCargaResposta reservarGate(@Valid @RequestBody ReservarGateCargaRequest request) {
        return servico.reservarGate(request);
    }

    @GetMapping("/gate/reservas")
    @Operation(summary = "Listar reservas de carga por agendamento")
    public List<ReservaGateCargaResposta> listarReservasGate(
            @RequestParam String agendamentoCodigo) {
        return servico.listarReservasGate(agendamentoCodigo);
    }

    @PostMapping("/gate/reservas/{reservaId}/confirmar")
    @Operation(summary = "Confirmar reserva após estágio físico do Gate")
    public ReservaGateCargaResposta confirmarGate(
            @PathVariable UUID reservaId,
            @Valid @RequestBody ConfirmarGateCargaRequest request) {
        return servico.confirmarGate(reservaId, request);
    }

    @PostMapping("/gate/reservas/{reservaId}/compensar")
    @Operation(summary = "Compensar reserva ou estoque confirmado pelo Gate")
    public ReservaGateCargaResposta compensarGate(
            @PathVariable UUID reservaId,
            @Valid @RequestBody CompensarGateCargaRequest request) {
        return servico.compensarGate(reservaId, request);
    }

    @PostMapping("/alocacoes")
    @Operation(summary = "Criar allocation com reserva de capacidade")
    public AlocacaoResposta criarAlocacao(@Valid @RequestBody CriarAlocacaoRequest request) {
        return servico.criarAlocacao(request);
    }

    @GetMapping("/alocacoes")
    @Operation(summary = "Listar allocations de um cargo lot")
    public List<AlocacaoResposta> listarAlocacoes(@RequestParam UUID loteId) {
        return servico.listarAlocacoes(loteId);
    }

    @PostMapping("/alocacoes/{id}/confirmar")
    @Operation(summary = "Confirmar allocation após movimento físico")
    public AlocacaoResposta confirmarAlocacao(
            @PathVariable UUID id,
            @Valid @RequestBody ComandoMotivadoRequest request) {
        return servico.confirmarAlocacao(id, request);
    }

    @PostMapping("/alocacoes/{id}/cancelar")
    @Operation(summary = "Cancelar allocation e liberar capacidade")
    public AlocacaoResposta cancelarAlocacao(
            @PathVariable UUID id,
            @Valid @RequestBody ComandoMotivadoRequest request) {
        return servico.cancelarAlocacao(id, request);
    }

    @PostMapping("/planos-transporte")
    @Operation(summary = "Planejar carga ou descarga por navio ou ferrovia")
    public PlanoTransporteResposta planejarTransporte(
            @Valid @RequestBody PlanejarTransporteRequest request) {
        return servico.planejarTransporte(request);
    }

    @GetMapping("/planos-transporte")
    @Operation(summary = "Listar plano modal por visita")
    public List<PlanoTransporteResposta> listarPlanosTransporte(
            @RequestParam ModalTransporteCargo modal,
            @RequestParam String visitaId) {
        return servico.listarPlanosTransporte(modal, visitaId);
    }

    @PostMapping("/planos-transporte/{id}/executar")
    @Operation(summary = "Confirmar execução física integral do saldo planejado")
    public PlanoTransporteResposta executarTransporte(
            @PathVariable UUID id,
            @Valid @RequestBody ExecutarTransporteRequest request) {
        return servico.executarTransporte(id, request);
    }

    @PostMapping("/planos-transporte/{id}/cancelar")
    @Operation(summary = "Cancelar plano modal sem execução física")
    public PlanoTransporteResposta cancelarTransporte(
            @PathVariable UUID id,
            @Valid @RequestBody ComandoMotivadoRequest request) {
        return servico.cancelarTransporte(id, request);
    }

    @PostMapping("/avarias")
    @Operation(summary = "Abrir avaria e segregar saldo afetado")
    public AvariaResposta abrirAvaria(@Valid @RequestBody AbrirAvariaRequest request) {
        return servico.abrirAvaria(request);
    }

    @GetMapping("/avarias")
    @Operation(summary = "Listar avarias de um cargo lot")
    public List<AvariaResposta> listarAvarias(@RequestParam UUID loteId) {
        return servico.listarAvarias(loteId);
    }

    @PostMapping("/avarias/{id}/evidencias")
    @Operation(summary = "Adicionar evidência à avaria")
    public AvariaResposta adicionarEvidencia(
            @PathVariable UUID id,
            @Valid @RequestBody AdicionarEvidenciaAvariaRequest request) {
        return servico.adicionarEvidencia(id, request);
    }

    @PostMapping("/avarias/{id}/transicoes")
    @Operation(summary = "Inspecionar, reparar, concluir reparo ou baixar avaria")
    public AvariaResposta transicionarAvaria(
            @PathVariable UUID id,
            @Valid @RequestBody TransicionarAvariaRequest request) {
        return servico.transicionarAvaria(id, request);
    }

    @PostMapping("/identificacoes")
    @Operation(summary = "Registrar código de barras ou QR de um cargo lot")
    public IdentificacaoResposta registrarIdentificacao(
            @Valid @RequestBody RegistrarIdentificacaoRequest request) {
        return identificacaoServico.registrar(request);
    }

    @GetMapping("/identificacoes/resolver")
    @Operation(summary = "Resolver código de barras ou QR")
    public IdentificacaoResposta resolverIdentificacao(@RequestParam String codigo) {
        return identificacaoServico.resolver(codigo);
    }

    @GetMapping("/identificacoes")
    @Operation(summary = "Listar identificações de um cargo lot")
    public List<IdentificacaoResposta> listarIdentificacoes(@RequestParam UUID loteId) {
        return identificacaoServico.listar(loteId);
    }

    @PostMapping("/inventarios")
    @Operation(summary = "Abrir inventário físico por posição")
    public InventarioFisicoResposta abrirInventario(
            @Valid @RequestBody AbrirInventarioFisicoRequest request) {
        return servico.abrirInventario(request);
    }

    @GetMapping("/inventarios")
    @Operation(summary = "Listar sessões de inventário físico")
    public List<InventarioFisicoResposta> listarInventarios() {
        return servico.listarInventarios();
    }

    @PostMapping("/inventarios/{id}/contagens")
    @Operation(summary = "Registrar contagem por código de barras ou QR")
    public InventarioFisicoResposta registrarContagem(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarContagemRequest request) {
        UUID loteIdentificado = identificacaoServico.resolverLoteId(request.identificacao());
        if (!loteIdentificado.equals(request.loteId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A identificação física pertence a outro cargo lot.");
        }
        RegistrarContagemRequest requestNormalizado = new RegistrarContagemRequest(
                request.commandId(),
                request.loteId(),
                identificacaoServico.resolverCodigoLote(request.identificacao()),
                request.quantidadeContada(),
                request.volumeContadoM3(),
                request.pesoContadoKg(),
                request.usuario(),
                request.observacao());
        return servico.registrarContagem(id, requestNormalizado);
    }

    @PostMapping("/inventarios/{id}/divergencias")
    @Operation(summary = "Aprovar ajuste motivado ou rejeitar divergência")
    public InventarioFisicoResposta resolverDivergencia(
            @PathVariable UUID id,
            @Valid @RequestBody ResolverDivergenciaRequest request) {
        return servico.resolverDivergencia(id, request);
    }

    @PostMapping("/inventarios/{id}/concluir")
    @Operation(summary = "Concluir inventário sem divergências pendentes")
    public InventarioFisicoResposta concluirInventario(
            @PathVariable UUID id,
            @Valid @RequestBody ComandoMotivadoRequest request) {
        return servico.concluirInventario(id, request);
    }
}
