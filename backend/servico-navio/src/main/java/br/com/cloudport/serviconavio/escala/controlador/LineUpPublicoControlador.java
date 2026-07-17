package br.com.cloudport.serviconavio.escala.controlador;

import br.com.cloudport.serviconavio.escala.dto.LineUpPublicoDTO;
import br.com.cloudport.serviconavio.escala.servico.EscalaServico;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@Validated
public class LineUpPublicoControlador {

    private final EscalaServico escalaServico;

    public LineUpPublicoControlador(EscalaServico escalaServico) {
        this.escalaServico = escalaServico;
    }

    @GetMapping("/public/line-up-navios")
    public ResponseEntity<List<LineUpPublicoDTO>> listar(
            @RequestParam(name = "dias", defaultValue = "30") int dias) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(escalaServico.listarLineUpPublico(dias));
    }
}
