package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.AtualizarRestricaoPilhaDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerMapaDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.EquipamentoMapaDto;
import br.com.cloudport.servicoyard.patio.dto.EquipamentoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.FiltrosMapaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioFiltro;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.dto.MovimentarConteinerPatioDto;
import br.com.cloudport.servicoyard.patio.dto.MovimentoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.OpcoesCadastroPatioDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoPatioDto;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import br.com.cloudport.servicoyard.patio.servico.OperacaoGraficaPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio")
public class MapaPatioControlador {

    private static final String AUTORIZACAO_OPERACAO_PATIO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')";

    private final MapaPatioServico mapaPatioServico;
    private final OperacaoGraficaPatioServico operacaoGraficaPatioServico;

    public MapaPatioControlador(MapaPatioServico mapaPatioServico,
                                 OperacaoGraficaPatioServico operacaoGraficaPatioServico) {
        this.mapaPatioServico = mapaPatioServico;
        this.operacaoGraficaPatioServico = operacaoGraficaPatioServico;
    }

    @GetMapping("/mapa")
    public MapaPatioRespostaDto consultarMapa(@RequestParam(name = "status", required = false) List<String> status,
                                               @RequestParam(name = "tipoCarga", required = false) List<String> tiposCarga,
                                               @RequestParam(name = "destino", required = false) List<String> destinos,
                                               @RequestParam(name = "camada", required = false) List<String> camadas,
                                               @RequestParam(name = "tipoEquipamento", required = false) List<String> tiposEquipamento) {
        MapaPatioFiltro filtro = mapaPatioServico.construirFiltro(status, tiposCarga, destinos, camadas, tiposEquipamento);
        return mapaPatioServico.consultarMapa(filtro);
    }

    @GetMapping("/filtros")
    public FiltrosMapaPatioDto consultarFiltros() {
        return mapaPatioServico.consultarFiltros();
    }

    @GetMapping("/posicoes")
    public List<PosicaoPatioDto> listarPosicoes() {
        return mapaPatioServico.listarPosicoes();
    }

    @GetMapping("/conteineres")
    public List<ConteinerMapaDto> listarConteineres() {
        return mapaPatioServico.listarConteineres();
    }

    @GetMapping("/movimentacoes")
    public List<MovimentoPatioDto> listarMovimentacoes() {
        return mapaPatioServico.listarMovimentacoesRecentes();
    }

    @GetMapping("/opcoes")
    public OpcoesCadastroPatioDto consultarOpcoesCadastro() {
        return mapaPatioServico.consultarOpcoesCadastro();
    }

    @PostMapping("/conteineres")
    public ConteinerMapaDto registrarOuAtualizarConteiner(@Valid @RequestBody ConteinerPatioRequisicaoDto requisicaoDto) {
        return mapaPatioServico.registrarOuAtualizarConteiner(requisicaoDto);
    }

    @PostMapping("/conteineres/{id}/movimentar")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public ConteinerMapaDto movimentarConteiner(@PathVariable Long id,
                                                 @Valid @RequestBody MovimentarConteinerPatioDto requisicaoDto) {
        return operacaoGraficaPatioServico.movimentar(id, requisicaoDto);
    }

    @PatchMapping("/posicoes/{id}/restricao-pilha")
    @PreAuthorize(AUTORIZACAO_OPERACAO_PATIO)
    public ResponseEntity<Void> atualizarRestricaoPilha(@PathVariable Long id,
                                                         @Valid @RequestBody AtualizarRestricaoPilhaDto requisicaoDto) {
        operacaoGraficaPatioServico.atualizarRestricao(id, requisicaoDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/equipamentos")
    public EquipamentoMapaDto registrarOuAtualizarEquipamento(@Valid @RequestBody EquipamentoPatioRequisicaoDto requisicaoDto) {
        return mapaPatioServico.registrarOuAtualizarEquipamento(requisicaoDto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErrosDeNegocio(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}