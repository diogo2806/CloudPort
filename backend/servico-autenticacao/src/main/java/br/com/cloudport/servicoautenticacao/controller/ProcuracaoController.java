package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.ProcuracaoDTO;
import br.com.cloudport.servicoautenticacao.service.ProcuracaoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procuracoes")
public class ProcuracaoController {

    private final ProcuracaoService procuracaoService;

    public ProcuracaoController(ProcuracaoService procuracaoService) {
        this.procuracaoService = procuracaoService;
    }

    @GetMapping
    public ResponseEntity<List<ProcuracaoDTO>> listarTodasProcuracoes() {
        List<ProcuracaoDTO> procuracoes = procuracaoService.listarTodasProcuracoes();
        return new ResponseEntity<>(procuracoes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcuracaoDTO> encontrarProcuracaoPorId(@PathVariable Long id) {
        ProcuracaoDTO procuracao = procuracaoService.encontrarProcuracaoPorId(id);
        if (procuracao == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(procuracao, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ProcuracaoDTO> salvarProcuracao(@RequestBody ProcuracaoDTO novaProcuracao) {
        ProcuracaoDTO procuracao = procuracaoService.salvarProcuracao(novaProcuracao);
        return new ResponseEntity<>(procuracao, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarProcuracao(@PathVariable Long id) {
        procuracaoService.deletarProcuracao(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
