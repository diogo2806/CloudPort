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
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import java.io.IOException;
import java.net.URI;
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

    private final EdiAuditoriaServico auditoria;
    private final BayPlanServico bayPlanServico;

    public EdiIntegracaoControlador(EdiAuditoriaServico auditoria, BayPlanServico bayPlanServico) {
        this.auditoria = auditoria;
        this.bayPlanServico = bayPlanServico;
    }

    @PostMapping(value = "/baplie/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessamentoEdiRespostaDto> uploadBaplie(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) throws IOException {
        if (arquivo.isEmpty()) throw new IllegalArgumentException("BAPLIE: o arquivo enviado esta vazio.");
        String conteudo = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        return respostaAceita(auditoria.registrarRecebimento(TipoMensagemEdi.BAPLIE, conteudo, null, null,
                arquivo.getOriginalFilename(), correlationId));
    }

    @PostMapping(value = "/baplie/texto", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ProcessamentoEdiRespostaDto> receberBaplieTexto(
            @RequestBody String conteudoEdifact,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        return respostaAceita(auditoria.registrarRecebimento(TipoMensagemEdi.BAPLIE, conteudoEdifact,
                null, null, null, correlationId));
    }

    @PostMapping("/coprar")
    public ResponseEntity<ProcessamentoEdiRespostaDto> receberCoprar(
            @Valid @RequestBody CoprarMensagemDto dto,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        return respostaAceita(auditoria.registrarRecebimento(TipoMensagemEdi.COPRAR, dto.getConteudoEdifact(),
                dto.getCodigoNavio(), dto.getCodigoViagem(), dto.getReferenciaMensagem(), correlationId));
    }

    @PostMapping("/coarri")
    public ResponseEntity<ProcessamentoEdiRespostaDto> receberCoarri(
            @Valid @RequestBody CoarriMensagemDto dto,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        return respostaAceita(auditoria.registrarRecebimento(TipoMensagemEdi.COARRI, dto.getConteudoEdifact(),
                dto.getCodigoNavio(), dto.getCodigoViagem(), dto.getReferenciaMensagem(), correlationId));
    }

    @PostMapping("/vermas")
    public ResponseEntity<ProcessamentoEdiRespostaDto> receberVermas(
            @Valid @RequestBody VermasMensagemDto dto,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        return respostaAceita(auditoria.registrarRecebimento(TipoMensagemEdi.VERMAS, dto.getConteudoEdifact(),
                dto.getCodigoNavio(), dto.getCodigoViagem(), dto.getReferenciaMensagem(), correlationId));
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
    public ResponseEntity<ProcessamentoEdiRespostaDto> reprocessar(
            @PathVariable Long id, @Valid @RequestBody ComandoMotivadoDto comando,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        preencherCorrelationId(comando, correlationId);
        return respostaAceita(auditoria.reprocessar(id, comando));
    }

    @PostMapping("/processamentos/{id}/quarentena")
    public ResponseEntity<ProcessamentoEdiRespostaDto> colocarEmQuarentena(
            @PathVariable Long id, @Valid @RequestBody ComandoMotivadoDto comando,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        preencherCorrelationId(comando, correlationId);
        return ResponseEntity.ok(auditoria.colocarEmQuarentena(id, comando));
    }

    @PostMapping("/processamentos/{id}/cancelar")
    public ResponseEntity<ProcessamentoEdiRespostaDto> cancelar(
            @PathVariable Long id, @Valid @RequestBody ComandoMotivadoDto comando,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        preencherCorrelationId(comando, correlationId);
        return ResponseEntity.ok(auditoria.cancelar(id, comando));
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

    private void preencherCorrelationId(ComandoMotivadoDto comando, String correlationId) {
        if (comando.getCorrelationId() == null || comando.getCorrelationId().isBlank()) comando.setCorrelationId(correlationId);
    }

    private ResponseEntity<ProcessamentoEdiRespostaDto> respostaAceita(ProcessamentoEdiRespostaDto processamento) {
        return ResponseEntity.accepted()
                .location(URI.create("/api/edi/processamentos/" + processamento.id()))
                .header(HEADER_PROCESSAMENTO_ID, processamento.id().toString())
                .body(processamento);
    }
}
