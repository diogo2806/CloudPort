package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.DocumentoDTO;
import br.com.cloudport.servicoautenticacao.service.DocumentoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentoDTO>> listarTodosDocumentos() {
        List<DocumentoDTO> documentos = documentoService.listarTodosDocumentos();
        return new ResponseEntity<>(documentos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDTO> encontrarDocumentoPorId(@PathVariable Long id) {
        DocumentoDTO documento = documentoService.encontrarDocumentoPorId(id);
        if(documento == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(documento, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<DocumentoDTO> salvarDocumento(@RequestBody DocumentoDTO novoDocumento) {
        DocumentoDTO documento = documentoService.salvarDocumento(novoDocumento);
        return new ResponseEntity<>(documento, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarDocumento(@PathVariable Long id) {
        documentoService.deletarDocumento(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
