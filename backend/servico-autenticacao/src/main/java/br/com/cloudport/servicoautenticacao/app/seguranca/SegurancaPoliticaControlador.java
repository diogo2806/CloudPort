package br.com.cloudport.servicoautenticacao.app.seguranca;

import br.com.cloudport.servicoautenticacao.app.seguranca.dto.DiretrizSegurancaDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/configuracoes/seguranca")
public class SegurancaPoliticaControlador {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegurancaPoliticaControlador.class);

    private final SegurancaPoliticaConsultaServico segurancaPoliticaConsultaServico;
    private final ValidadorParametroSeguranca validadorParametroSeguranca;

    public SegurancaPoliticaControlador(
            SegurancaPoliticaConsultaServico segurancaPoliticaConsultaServico,
            ValidadorParametroSeguranca validadorParametroSeguranca
    ) {
        this.segurancaPoliticaConsultaServico = segurancaPoliticaConsultaServico;
        this.validadorParametroSeguranca = validadorParametroSeguranca;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<List<DiretrizSegurancaDTO>> listarPoliticas(
            @RequestParam(value = "versao", required = false) String versao,
            @RequestParam(value = "ordenacao", required = false) String ordenacao
    ) {
        try {
            validadorParametroSeguranca.validarParametroOpcional(versao, "versao");
            validadorParametroSeguranca.validarParametroOpcional(ordenacao, "ordenacao");
            return ResponseEntity.ok(segurancaPoliticaConsultaServico.listarDiretrizes(versao, ordenacao));
        } catch (ParametroSegurancaInvalidoException ex) {
            LOGGER.warn("Tentativa de injeção bloqueada nas configurações de segurança: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
