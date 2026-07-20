package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.dto.ExcluirGeometriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.GeometriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.GeometriaPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.servico.GeometriaPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/geometrias")
public class GeometriaPatioControlador {

    private static final String AUTORIZACAO_EDICAO = "hasAnyRole('ROOT','ADMIN_PORTO','PLANEJADOR')";

    private final GeometriaPatioServico geometriaPatioServico;

    public GeometriaPatioControlador(GeometriaPatioServico geometriaPatioServico) {
        this.geometriaPatioServico = geometriaPatioServico;
    }

    @GetMapping
    public List<GeometriaPatioDto> listar() {
        return geometriaPatioServico.listarAtivas();
    }

    @PostMapping
    @PreAuthorize(AUTORIZACAO_EDICAO)
    public ResponseEntity<GeometriaPatioDto> criar(
            @Valid @RequestBody GeometriaPatioRequisicaoDto requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(geometriaPatioServico.criar(requisicao));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AUTORIZACAO_EDICAO)
    public GeometriaPatioDto atualizar(@PathVariable Long id,
                                        @Valid @RequestBody GeometriaPatioRequisicaoDto requisicao) {
        return geometriaPatioServico.atualizar(id, requisicao);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AUTORIZACAO_EDICAO)
    public ResponseEntity<Void> desativar(@PathVariable Long id,
                                           @Valid @RequestBody ExcluirGeometriaPatioDto requisicao) {
        geometriaPatioServico.desativar(id, requisicao);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErroDeNegocio(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
