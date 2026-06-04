package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dto.NavioSiderurgicoDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.NavioSiderurgicoServico;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/navios-siderurgicos")
public class NavioSiderurgicoControlador {

    private final NavioSiderurgicoServico servico;

    public NavioSiderurgicoControlador(NavioSiderurgicoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<NavioSiderurgicoDTO> listar() {
        return servico.listar();
    }

    @PostMapping
    public ResponseEntity<NavioSiderurgicoDTO> criar(@Valid @RequestBody NavioSiderurgicoDTO dto) {
        NavioSiderurgicoDTO criado = servico.criar(dto);
        return ResponseEntity.created(URI.create("/navios-siderurgicos/" + criado.id())).body(criado);
    }
}
