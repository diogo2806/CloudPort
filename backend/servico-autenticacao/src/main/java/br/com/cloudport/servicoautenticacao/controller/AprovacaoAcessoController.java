package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.SolicitacaoAcessoDTO;
import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.service.SolicitacaoAcessoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AprovacaoAcessoController {

    private final SolicitacaoAcessoService solicitacaoAcessoService;

    public AprovacaoAcessoController(SolicitacaoAcessoService solicitacaoAcessoService) {
        this.solicitacaoAcessoService = solicitacaoAcessoService;
    }

    @GetMapping("/solicitacoes")
    @PreAuthorize("hasRole('ROLE_APROVADOR')")
    public ResponseEntity<List<SolicitacaoAcessoDTO>> listarSolicitacoesPendentes() {
        List<SolicitacaoAcesso> solicitacoes = solicitacaoAcessoService.listarSolicitacoesPendentes();
        return new ResponseEntity<>(solicitacoes.stream().map(SolicitacaoAcesso::toDTO).collect(Collectors.toList()), HttpStatus.OK);
    }

    @PostMapping("/solicitacoes/{id}/aprovar")
    @PreAuthorize("hasRole('ROLE_APROVADOR')")
    public ResponseEntity<Void> aprovarSolicitacao(@PathVariable Long id) {
        solicitacaoAcessoService.aprovarSolicitacao(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
