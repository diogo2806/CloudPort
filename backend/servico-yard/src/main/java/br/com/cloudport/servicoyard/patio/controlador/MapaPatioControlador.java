package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.ConteinerMapaDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.EquipamentoMapaDto;
import br.com.cloudport.servicoyard.patio.dto.EquipamentoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.FiltrosMapaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioFiltro;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio")
public class MapaPatioControlador {

    private final MapaPatioServico mapaPatioServico;

    public MapaPatioControlador(MapaPatioServico mapaPatioServico) {
        this.mapaPatioServico = mapaPatioServico;
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

    @PostMapping("/conteineres")
    public ConteinerMapaDto registrarOuAtualizarConteiner(@Valid @RequestBody ConteinerPatioRequisicaoDto requisicaoDto) {
        return mapaPatioServico.registrarOuAtualizarConteiner(requisicaoDto);
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
