package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.EntradaPessoaRequest;
import br.com.cloudport.servicogate.app.gestor.dto.MovimentacaoPessoaDTO;
import br.com.cloudport.servicogate.app.gestor.dto.PessoaPresenteDTO;
import br.com.cloudport.servicogate.app.gestor.dto.ResumoAcessoPessoasDTO;
import br.com.cloudport.servicogate.app.gestor.dto.SaidaPessoaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/pessoas")
@Tag(name = "Controle de Pessoas no Gate", description = "Entrada, saída, presença atual e histórico de pessoas no terminal")
public class ControleAcessoPessoasController {

    private final ControleAcessoPessoasService controleAcessoPessoasService;

    public ControleAcessoPessoasController(ControleAcessoPessoasService controleAcessoPessoasService) {
        this.controleAcessoPessoasService = controleAcessoPessoasService;
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar a entrada de uma pessoa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrada registrada"),
            @ApiResponse(responseCode = "409", description = "A entrada já foi consumida por outra requisição")
    })
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public MovimentacaoPessoaDTO registrarEntrada(@Valid @RequestBody EntradaPessoaRequest request) {
        return controleAcessoPessoasService.registrarEntrada(request);
    }

    @PostMapping("/saidas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar a saída de uma pessoa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Saída registrada"),
            @ApiResponse(responseCode = "409", description = "A saída já foi consumida por outra requisição")
    })
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE')")
    public MovimentacaoPessoaDTO registrarSaida(@Valid @RequestBody SaidaPessoaRequest request) {
        return controleAcessoPessoasService.registrarSaida(request);
    }

    @GetMapping("/presentes")
    @Operation(summary = "Listar as pessoas atualmente presentes no terminal")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public List<PessoaPresenteDTO> listarPresentes() {
        return controleAcessoPessoasService.listarPresentes();
    }

    @GetMapping("/resumo")
    @Operation(summary = "Obter o total de pessoas presentes por tipo")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public ResumoAcessoPessoasDTO obterResumo() {
        return controleAcessoPessoasService.obterResumo();
    }

    @GetMapping("/movimentacoes")
    @Operation(summary = "Consultar o histórico recente de entradas e saídas")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_GATE','PLANEJADOR')")
    public List<MovimentacaoPessoaDTO> listarMovimentacoes(
            @RequestParam(required = false) String documento,
            @RequestParam(defaultValue = "100") int limite) {
        return controleAcessoPessoasService.listarMovimentacoes(documento, limite);
    }
}
