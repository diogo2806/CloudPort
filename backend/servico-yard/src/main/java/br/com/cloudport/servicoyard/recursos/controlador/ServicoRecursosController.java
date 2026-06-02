package br.com.cloudport.servicoyard.recursos.controlador;

import br.com.cloudport.servicoyard.recursos.dto.BercoResumoDTO;
import br.com.cloudport.servicoyard.recursos.dto.CalendarioBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.EquipamentoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.ReservaBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.RespostaAlocacaoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.ResumoRecursosDTO;
import br.com.cloudport.servicoyard.recursos.dto.SolicitacaoAlocacaoBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.SolicitacaoManutencaoBercoDTO;
import br.com.cloudport.servicoyard.recursos.servico.ServicoRecursosService;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/yard/recursos")
public class ServicoRecursosController {

    private final ServicoRecursosService servicoRecursosService;

    public ServicoRecursosController(ServicoRecursosService servicoRecursosService) {
        this.servicoRecursosService = servicoRecursosService;
    }

    @GetMapping("/bercos")
    public List<BercoResumoDTO> listarBercos() {
        return servicoRecursosService.listarBercos();
    }

    @GetMapping("/calendario")
    public List<CalendarioBercoDTO> consultarCalendario(
            @RequestParam(name = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "dias", defaultValue = "14") int dias) {
        return servicoRecursosService.consultarCalendario(inicio, dias);
    }

    @GetMapping("/resumo")
    public ResumoRecursosDTO consultarResumo() {
        return servicoRecursosService.consultarResumo();
    }

    @GetMapping("/reservas")
    public List<ReservaBercoDTO> listarReservas() {
        return servicoRecursosService.listarReservas();
    }

    @GetMapping("/equipamentos")
    public List<EquipamentoBercoDTO> listarEquipamentos() {
        return servicoRecursosService.listarEquipamentos();
    }

    @PostMapping("/alocacoes")
    public RespostaAlocacaoBercoDTO alocarNavio(@Valid @RequestBody SolicitacaoAlocacaoBercoDTO solicitacao) {
        return servicoRecursosService.recomendarOuConfirmarAlocacao(solicitacao);
    }

    @PostMapping("/manutencoes")
    public ReservaBercoDTO agendarManutencao(@Valid @RequestBody SolicitacaoManutencaoBercoDTO solicitacao) {
        return servicoRecursosService.agendarManutencao(solicitacao);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErros(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
