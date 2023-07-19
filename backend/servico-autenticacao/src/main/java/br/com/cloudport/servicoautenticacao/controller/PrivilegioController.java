import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.com.cloudport.servicoautenticacao.model.Privilegio;
import br.com.cloudport.servicoautenticacao.service.PrivilegioService;

import java.util.List;

@RestController
@RequestMapping("/api/privilegios")
public class PrivilegioController {

    private final PrivilegioService privilegioService;

    @Autowired
    public PrivilegioController(PrivilegioService privilegioService) {
        this.privilegioService = privilegioService;
    }

    @GetMapping
    public ResponseEntity<List<Privilegio>> getAllPrivilegios() {
        return ResponseEntity.ok(privilegioService.getAllPrivilegios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Privilegio> getPrivilegioById(@PathVariable Long id) {
        return ResponseEntity.ok(privilegioService.getPrivilegioById(id));
    }

    @PostMapping
    public ResponseEntity<Privilegio> createPrivilegio(@RequestBody Privilegio privilegio) {
        return ResponseEntity.ok(privilegioService.createPrivilegio(privilegio));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Privilegio> updatePrivilegio(@PathVariable Long id, @RequestBody Privilegio privilegio) {
        privilegio.setId(id);
        return ResponseEntity.ok(privilegioService.updatePrivilegio(privilegio));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrivilegio(@PathVariable Long id) {
        privilegioService.deletePrivilegio(id);
        return ResponseEntity.ok().build();
    }

    // Você pode adicionar mais métodos aqui, se necessário
}
