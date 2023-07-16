package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.dto.EmpresaDTO;
import br.com.cloudport.servicoautenticacao.service.EmpresaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaDTO>> listarTodasEmpresas() {
        List<EmpresaDTO> empresas = empresaService.listarTodasEmpresas();
        return new ResponseEntity<>(empresas, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaDTO> encontrarEmpresaPorId(@PathVariable Long id) {
        EmpresaDTO empresa = empresaService.encontrarEmpresaPorId(id);
        if (empresa == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(empresa, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<EmpresaDTO> salvarEmpresa(@RequestBody EmpresaDTO novaEmpresa) {
        EmpresaDTO empresa = empresaService.salvarEmpresa(novaEmpresa);
        return new ResponseEntity<>(empresa, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEmpresa(@PathVariable Long id) {
        empresaService.deletarEmpresa(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
