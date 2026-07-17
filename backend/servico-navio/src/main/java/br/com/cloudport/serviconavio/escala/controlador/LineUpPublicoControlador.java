package br.com.cloudport.serviconavio.escala.controlador;

import br.com.cloudport.serviconavio.escala.dto.LineUpPublicoDTO;
import br.com.cloudport.serviconavio.escala.dto.LinhaUpEscalaDTO;
import br.com.cloudport.serviconavio.escala.servico.EscalaServico;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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
        List<LineUpPublicoDTO> lineUp = escalaServico.listarLineUp(dias).stream()
                .map(this::mapearPublico)
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(lineUp);
    }

    private LineUpPublicoDTO mapearPublico(LinhaUpEscalaDTO escala) {
        String berco = StringUtils.hasText(escala.getBercoAtual())
                ? escala.getBercoAtual()
                : escala.getBercoPrevisto();
        return new LineUpPublicoDTO(
                escala.getNomeNavio(),
                escala.getCodigoImo(),
                escala.getViagemEntrada(),
                escala.getViagemSaida(),
                escala.getFase(),
                escala.getChegadaPrevista(),
                escala.getAtracacaoPrevista(),
                escala.getPartidaPrevista(),
                escala.getChegadaEfetiva(),
                escala.getAtracacaoEfetiva(),
                escala.getPartidaEfetiva(),
                berco,
                null
        );
    }
}
