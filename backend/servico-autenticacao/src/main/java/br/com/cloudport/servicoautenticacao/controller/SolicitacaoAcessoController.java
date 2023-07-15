package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.SolicitacaoAcessoDTO;
import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.service.SolicitacaoAcessoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SolicitacaoAcessoController {

    private final SolicitacaoAcessoService solicitacaoAcessoService;

    public SolicitacaoAcessoController(SolicitacaoAcessoService solicitacaoAcessoService) {
        this.solicitacaoAcessoService = solicitacaoAcessoService;
    }

    @PostMapping("/solicitacoes")
    public ResponseEntity<SolicitacaoAcessoDTO> criarSolicitacao(@RequestBody SolicitacaoAcessoDTO novaSolicitacao) {
        SolicitacaoAcesso solicitacaoAcesso = solicitacaoAcessoService.salvarSolicitacao(novaSolicitacao);
        return new ResponseEntity<>(solicitacaoAcesso.toDTO(), HttpStatus.CREATED);
    }
}
