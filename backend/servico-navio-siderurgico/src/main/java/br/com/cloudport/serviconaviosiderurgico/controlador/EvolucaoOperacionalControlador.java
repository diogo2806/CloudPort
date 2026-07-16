package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dto.ConfiguracaoRestricoesEstruturaisDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OtimizacaoGlobalNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoEstruturalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.AnaliseOperacionalAvancadaServico;
import br.com.cloudport.serviconaviosiderurgico.servico.EventoOperacionalStreamingServico;
import br.com.cloudport.serviconaviosiderurgico.servico.OtimizacaoGlobalNavioPatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.RelatorioOperacionalExportacaoServico;
import br.com.cloudport.serviconaviosiderurgico.servico.ValidacaoEstruturalNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import javax.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/visitas-navio/{id}")
public class EvolucaoOperacionalControlador {

    private final VisitaNavioServico visitaServico;
    private final EventoOperacionalStreamingServico eventoStreamingServico;
    private final AnaliseOperacionalAvancadaServico analiseOperacionalServico;
    private final OtimizacaoGlobalNavioPatioServico otimizacaoGlobalServico;
    private final ValidacaoEstruturalNavioServico validacaoEstruturalServico;
    private final RelatorioOperacionalExportacaoServico relatorioExportacaoServico;

    public EvolucaoOperacionalControlador(
            VisitaNavioServico visitaServico,
            EventoOperacionalStreamingServico eventoStreamingServico,
            AnaliseOperacionalAvancadaServico analiseOperacionalServico,
            OtimizacaoGlobalNavioPatioServico otimizacaoGlobalServico,
            ValidacaoEstruturalNavioServico validacaoEstruturalServico,
            RelatorioOperacionalExportacaoServico relatorioExportacaoServico
    ) {
        this.visitaServico = visitaServico;
        this.eventoStreamingServico = eventoStreamingServico;
        this.analiseOperacionalServico = analiseOperacionalServico;
        this.otimizacaoGlobalServico = otimizacaoGlobalServico;
        this.validacaoEstruturalServico = validacaoEstruturalServico;
        this.relatorioExportacaoServico = relatorioExportacaoServico;
    }

    @PostMapping("/validacoes-estruturais")
    public ValidacaoEstruturalNavioDTO validarRestricoesEstruturais(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracaoRestricoesEstruturaisDTO configuracao
    ) {
        return validacaoEstruturalServico.validar(id, configuracao);
    }

    @GetMapping(value = "/eventos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEventos(
            @PathVariable Long id,
            @RequestHeader(name = "Last-Event-ID", required = false) String ultimoEventoId
    ) {
        visitaServico.buscarEntidade(id);
        return eventoStreamingServico.assinar(id, ultimoEventoId);
    }

    @GetMapping("/control-room")
    public PainelOperacionalAvancadoDTO controlRoom(@PathVariable Long id) {
        return analiseOperacionalServico.analisar(id);
    }

    @PostMapping("/otimizacao-global")
    public OtimizacaoGlobalNavioPatioDTO otimizarOperacao(@PathVariable Long id) {
        return otimizacaoGlobalServico.otimizar(id);
    }

    @GetMapping(value = "/relatorio-operacional-integrado.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> relatorioOperacionalCsv(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-operacional-visita-" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(relatorioExportacaoServico.gerarCsv(id));
    }

    @GetMapping(value = "/relatorio-operacional-integrado.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> relatorioOperacionalPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-operacional-visita-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(relatorioExportacaoServico.gerarPdf(id));
    }
}
