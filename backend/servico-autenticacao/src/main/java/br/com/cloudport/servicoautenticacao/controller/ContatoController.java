package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.ContatoDTO;
import br.com.cloudport.servicoautenticacao.service.ContatoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contatos")
public class ContatoController {

    private final ContatoService contatoService;

    public ContatoController(ContatoService contatoService) {
        this.contatoService = contatoService;
    }

    @GetMapping
    public ResponseEntity<List<ContatoDTO>> listarTodosContatos() {
        List<ContatoDTO> contatos = contatoService.listarTodosContatos();
        return new ResponseEntity<>(contatos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContatoDTO> encontrarContatoPorId(@PathVariable Long id) {
        ContatoDTO contato = contatoService.encontrarContatoPorId(id);
        if(contato == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(contato, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ContatoDTO> salvarContato(@RequestBody ContatoDTO novoContato) {
        ContatoDTO contato = contatoService.salvarContato(novoContato);
        return new ResponseEntity<>(contato, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarContato(@PathVariable Long id) {
        contatoService.deletarContato(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
