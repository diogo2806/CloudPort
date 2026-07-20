package br.com.cloudport.servicoyard.patio.custodia.controlador;

import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaComandoDto;
import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaRespostaDto;
import br.com.cloudport.servicoyard.patio.custodia.modelo.StatusCustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.servico.CustodiaExchangeAreaServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/yard/patio/exchange-areas/custodias")
public class CustodiaExchangeAreaControlador {

    private static final String AUTORIZACAO_CONSULTA =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')";
    private static final String AUTORIZACAO_OPERACAO =
            "hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO')";

    private final CustodiaExchangeAreaServico servico;

    public CustodiaExchangeAreaControlador(CustodiaExchangeAreaServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize(AUTORIZACAO_CONSULTA)
    public List<CustodiaExchangeAreaRespostaDto> listar(
            @RequestParam(name = "status", required = false) StatusCustodiaExchangeArea status) {
        return servico.listar(status);
    }

    @PostMapping("/entregas")
    @PreAuthorize(AUTORIZACAO_OPERACAO)
    public ResponseEntity<CustodiaExchangeAreaRespostaDto> entregar(
            @Valid @RequestBody CustodiaExchangeAreaComandoDto comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.entregarNaExchangeArea(comando));
    }

    @PostMapping("/{custodiaId}/recebimentos")
    @PreAuthorize(AUTORIZACAO_OPERACAO)
    public CustodiaExchangeAreaRespostaDto receber(
            @PathVariable Long custodiaId,
            @Valid @RequestBody CustodiaExchangeAreaComandoDto comando) {
        return servico.receberDaExchangeArea(custodiaId, comando);
    }
}
