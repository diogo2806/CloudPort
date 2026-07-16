package br.com.cloudport.servicoyard.edi.controlador;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.PaginaRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.VermasMensagemDto;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.servico.BayPlanServico;
import br.com.cloudport.servicoyard.edi.servico.EdiAuditoriaServico;
import br.com.cloudport.servicoyard.edi.servico.EdiProcessadorServico;
import br.com.cloudport.servicoyard.edi.servico.ResultadoProcessamentoEdi;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/edi")
public class EdiIntegracaoControlador {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String HEADER_PROCESSAMENTO_ID = "X-EDI-Processing-Id";

    private final EdiProcessadorServico processador;
    private final EdiAuditoriaServico auditoria;
    private final BayPlanServico bayPlanServico;

    public EdiIntegracaoControlador(EdiProcessadorServico processador,
                                     EdiAuditoriaServico auditoria,
                                     BayPlanServico bayPlanServico) {
        this.processador = processador;
        this.auditoria = auditoria;
        this.bayPlanServico = bayPlanServico;
    }

    @PostMapping(value = "/baplie/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BayPlanRespostaDto> uploadBaplie(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) throws IOException {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("BAPLIE: o arquivo enviado esta vazio.");
        }
        String conteudo = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        ResultadoProcessamentoEdi resultado = auditoria.processar(
                TipoMensagemEdi.BAPLIE,
                conteudo,
                null,
                null,
                arquivo.getOriginalFilename(),
                correlationId,
                () -> processador.processarBaplie(conteudo)
        );
        return resposta(resultado);
    }

    @PostMapping(value = "/baplie/texto", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<BayPlanRespostaDto> receberBaplieTexto(
            @RequestBody String conteudoEdifact,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        ResultadoProcessamentoEdi resultado = auditoria.processar(
                TipoMensagemEdi.BAPLIE,
                conteudoEdifact,
                null,
                null,
                null,
                correlationId,
                () -> processador.processarBaplie(conteudoEdifact)
        );
        return resposta(resultado);
    }

    @PostMapping("/coprar")
    public ResponseEntity<BayPlanRespostaDto> receberCoprar(
            @Valid @RequestBody CoprarMensagemDto dto,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        ResultadoProcessamentoEdi resultado = auditoria.processar(
                TipoMensagemEdi.COPRAR,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                correlationId,
                () -> processador.processarCoprar(dto)
        );
        return resposta(resultado);
    }

    @PostMapping("/coarri")
    public ResponseEntity<BayPlanRespostaDto> receberCoarri(
            @Valid @RequestBody CoarriMensagemDto dto,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        ResultadoProcessamentoEdi resultado = auditoria.processar(
                TipoMensagemEdi.COARRI,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                correlationId,
                () -> processador.processarCoarri(dto)
        );
        return resposta(resultado);
    }

    @PostMapping("/vermas")
    public ResponseEntity<BayPlanRespostaDto> receberVermas(
            @Valid @RequestBody VermasMensagemDto dto,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        ResultadoProcessamentoEdi resultado = auditoria.processar(
                TipoMensagemEdi.VERMAS,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                correlationId,
                () -> processador.processarVermas(dto)
        );
        return resposta(resultado);
    }

    @GetMapping("/processamentos")
    public PaginaRespostaDto<ProcessamentoEdiRespostaDto> listarProcessamentos(
            @RequestParam(required = false) TipoMensagemEdi tipo,
            @RequestParam(required = false) StatusProcessamentoEdi status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return auditoria.listar(tipo, status, pagina, tamanho);
    }

    @GetMapping("/processamentos/{id}")
    public ProcessamentoEdiRespostaDto buscarProcessamento(@PathVariable Long id) {
        return auditoria.buscar(id);
    }

    @PostMapping("/processamentos/{id}/reprocessar")
    public ResponseEntity<ResultadoProcessamentoEdi> reprocessar(
            @PathVariable Long id,
            @Valid @RequestBody ComandoMotivadoDto comando,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        if (comando.getCorrelationId() == null || comando.getCorrelationId().isBlank()) {
            comando.setCorrelationId(correlationId);
        }
        ResultadoProcessamentoEdi resultado = auditoria.reprocessar(id, comando);
        return ResponseEntity.ok()
                .header(HEADER_PROCESSAMENTO_ID, resultado.processamento().id().toString())
                .body(resultado);
    }

    @GetMapping("/bay-plan/{id}")
    public ResponseEntity<BayPlanRespostaDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bayPlanServico.buscarPorId(id));
    }

    @GetMapping("/bay-plan/navio/{codigoNavio}")
    public ResponseEntity<List<BayPlanRespostaDto>> buscarPorNavio(@PathVariable String codigoNavio) {
        return ResponseEntity.ok(bayPlanServico.buscarPorNavio(codigoNavio));
    }

    @GetMapping("/bay-plan/ativos")
    public ResponseEntity<List<BayPlanRespostaDto>> listarAtivos() {
        return ResponseEntity.ok(bayPlanServico.listarAtivos());
    }

    private ResponseEntity<BayPlanRespostaDto> resposta(ResultadoProcessamentoEdi resultado) {
        return ResponseEntity.ok()
                .header(HEADER_PROCESSAMENTO_ID, resultado.processamento().id().toString())
                .body(resultado.bayPlan());
    }
}
