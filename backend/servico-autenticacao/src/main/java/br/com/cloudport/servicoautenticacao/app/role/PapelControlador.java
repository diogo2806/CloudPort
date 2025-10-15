package br.com.cloudport.servicoautenticacao.app.role;

import br.com.cloudport.servicoautenticacao.app.role.dto.PapelDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class PapelControlador {

    private final PapelServico papelServico;

    public PapelControlador(PapelServico papelServico) {
        this.papelServico = papelServico;
    }

    @PostMapping
    public ResponseEntity<PapelDTO> criarPapel(@RequestBody PapelDTO papelDTO) {
        PapelDTO papelSalvo = papelServico.salvarPapel(papelDTO);
        return ResponseEntity.ok(papelSalvo);
    }

    @GetMapping("/{nome}")
    public ResponseEntity<PapelDTO> buscarPapel(@PathVariable String nome) {
        PapelDTO papel = papelServico.buscarPorNome(nome);
        return ResponseEntity.ok(papel);
    }

    @GetMapping
    public ResponseEntity<List<PapelDTO>> listarPapeis() {
        List<PapelDTO> papeis = papelServico.listarTodos();
        return ResponseEntity.ok(papeis);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PapelDTO> atualizarPapel(@PathVariable Long id, @RequestBody PapelDTO papelDTO) {
        PapelDTO papelAtualizado = papelServico.atualizar(id, papelDTO);
        return ResponseEntity.ok(papelAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerPapel(@PathVariable Long id) {
        papelServico.remover(id);
        return ResponseEntity.noContent().build();
    }
}
