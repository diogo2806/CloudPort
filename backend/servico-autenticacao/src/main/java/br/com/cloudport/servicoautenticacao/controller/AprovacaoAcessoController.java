package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.SolicitacaoAcessoDTO;
import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.service.SolicitacaoAcessoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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
        List<SolicitacaoAcessoDTO> solicitacoesDTO = solicitacoes.stream()
                .map(solicitacao -> new SolicitacaoAcessoDTO(solicitacao))
                .collect(Collectors.toList());
        return new ResponseEntity<>(solicitacoesDTO, HttpStatus.OK);
    }

    @PostMapping("/solicitacoes/{id}/aprovar")
    @PreAuthorize("hasRole('ROLE_APROVADOR')")
    public ResponseEntity<Void> aprovarSolicitacao(@PathVariable Long id) {
        solicitacaoAcessoService.aprovarSolicitacao(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
