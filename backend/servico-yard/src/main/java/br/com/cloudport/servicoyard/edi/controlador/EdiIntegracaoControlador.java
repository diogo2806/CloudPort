package br.com.cloudport.servicoyard.edi.controlador;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.servico.BayPlanServico;
import br.com.cloudport.servicoyard.edi.servico.EdiProcessadorServico;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API REST para integração EDI.
 *
 * Endpoints:
 *   POST /api/edi/baplie/upload          – upload de arquivo BAPLIE (multipart)
 *   POST /api/edi/baplie/texto           – BAPLIE como texto plain
 *   POST /api/edi/coprar                 – COPRAR via JSON (ou polling)
 *   POST /api/edi/coarri                 – COARRI via JSON
 *   GET  /api/edi/bay-plan/{id}          – consulta Bay Plan por ID
 *   GET  /api/edi/bay-plan/navio/{cod}   – todos os Bay Plans de um navio
 *   GET  /api/edi/bay-plan/ativos        – Bay Plans em operação
 */
@RestController
@RequestMapping("/api/edi")
public class EdiIntegracaoControlador {

    private final EdiProcessadorServico processador;
    private final BayPlanServico bayPlanServico;

    public EdiIntegracaoControlador(EdiProcessadorServico processador,
                                     BayPlanServico bayPlanServico) {
        this.processador = processador;
        this.bayPlanServico = bayPlanServico;
    }

    // ── BAPLIE ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/baplie/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BayPlanRespostaDto> uploadBaplie(
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        String conteudo = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(processador.processarBaplie(conteudo));
    }

    @PostMapping(value = "/baplie/texto", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<BayPlanRespostaDto> receberBaplieTexto(
            @RequestBody String conteudoEdifact) {
        return ResponseEntity.ok(processador.processarBaplie(conteudoEdifact));
    }

    // ── COPRAR ────────────────────────────────────────────────────────────────

    @PostMapping("/coprar")
    public ResponseEntity<BayPlanRespostaDto> receberCoprar(
            @RequestBody CoprarMensagemDto dto) {
        return ResponseEntity.ok(processador.processarCoprar(dto));
    }

    // ── COARRI ────────────────────────────────────────────────────────────────

    @PostMapping("/coarri")
    public ResponseEntity<BayPlanRespostaDto> receberCoarri(
            @RequestBody CoarriMensagemDto dto) {
        return ResponseEntity.ok(processador.processarCoarri(dto));
    }

    // ── Consultas Bay Plan ────────────────────────────────────────────────────

    @GetMapping("/bay-plan/{id}")
    public ResponseEntity<BayPlanRespostaDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bayPlanServico.buscarPorId(id));
    }

    @GetMapping("/bay-plan/navio/{codigoNavio}")
    public ResponseEntity<List<BayPlanRespostaDto>> buscarPorNavio(
            @PathVariable String codigoNavio) {
        return ResponseEntity.ok(bayPlanServico.buscarPorNavio(codigoNavio));
    }

    @GetMapping("/bay-plan/ativos")
    public ResponseEntity<List<BayPlanRespostaDto>> listarAtivos() {
        return ResponseEntity.ok(bayPlanServico.listarAtivos());
    }
}
