package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.AlertaIntegracaoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.AtualizarFaseVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.AtualizarStatusItemNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.BloqueioItemNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoGeracaoOrdensPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoGeracaoReservasPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPrioridadeOrdemPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ConfiguracaoRestricoesEstruturaisDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.FilaPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OtimizacaoGlobalNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PainelOperacionalAvancadoDTO.QuayMonitorDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PosicaoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.RelatorioOperacionalIntegradoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoGeracaoOrdensPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResumoIntegracaoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResumoOperacionalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoEstruturalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoPlanoEstivaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.AnaliseOperacionalAvancadaServico;
import br.com.cloudport.serviconaviosiderurgico.servico.EventoOperacionalStreamingServico;
import br.com.cloudport.serviconaviosiderurgico.servico.IntegracaoNavioPatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.ItemOperacaoNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.OperacaoOrdemPatioNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.OtimizacaoGlobalNavioPatioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.PlanoEstivaNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.RelatorioOperacionalExportacaoServico;
import br.com.cloudport.serviconaviosiderurgico.servico.ValidacaoEstruturalNavioServico;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/visitas-navio")
public class VisitaNavioControlador {

    private final VisitaNavioServico visitaServico;
    private final ItemOperacaoNavioServico itemServico;
    private final PlanoEstivaNavioServico planoServico;
    private final IntegracaoNavioPatioServico integracaoNavioPatioServico;
    private final OperacaoOrdemPatioNavioServico operacaoOrdemPatioNavioServico;
    private final EventoOperacionalStreamingServico eventoStreamingServico;
    private final AnaliseOperacionalAvancadaServico analiseOperacionalServico;
    private final OtimizacaoGlobalNavioPatioServico otimizacaoGlobalServico;
    private final ValidacaoEstruturalNavioServico validacaoEstruturalServico;
    private final RelatorioOperacionalExportacaoServico relatorioExportacaoServico;

    public VisitaNavioControlador(
            VisitaNavioServico visitaServico,
            ItemOperacaoNavioServico itemServico,
            PlanoEstivaNavioServico planoServico,
            IntegracaoNavioPatioServico integracaoNavioPatioServico,
            OperacaoOrdemPatioNavioServico operacaoOrdemPatioNavioServico,
            EventoOperacionalStreamingServico eventoStreamingServico,
            AnaliseOperacionalAvancadaServico analiseOperacionalServico,
            OtimizacaoGlobalNavioPatioServico otimizacaoGlobalServico,
            ValidacaoEstruturalNavioServico validacaoEstruturalServico,
            RelatorioOperacionalExportacaoServico relatorioExportacaoServico
    ) {
        this.visitaServico = visitaServico;
        this.itemServico = itemServico;
        this.planoServico = planoServico;
        this.integracaoNavioPatioServico = integracaoNavioPatioServico;
        this.operacaoOrdemPatioNavioServico = operacaoOrdemPatioNavioServico;
        this.eventoStreamingServico = eventoStreamingServico;
        this.analiseOperacionalServico = analiseOperacionalServico;
        this.otimizacaoGlobalServico = otimizacaoGlobalServico;
        this.validacaoEstruturalServico = validacaoEstruturalServico;
        this.relatorioExportacaoServico = relatorioExportacaoServico;
    }

    @GetMapping
    public List<VisitaNavioDTO> listar(
            @RequestParam(required = false) FaseVisitaNavio fase,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Long navioId
    ) {
        return visitaServico.listar(fase, dataInicio, dataFim, navioId);
    }

    @GetMapping("/{id}")
    public VisitaNavioDTO detalhar(@PathVariable Long id) {
        return visitaServico.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaNavioDTO criar(@Valid @RequestBody VisitaNavioDTO dto) {
        return visitaServico.criar(dto);
    }

    @PutMapping("/{id}")
    public VisitaNavioDTO atualizar(@PathVariable Long id, @Valid @RequestBody VisitaNavioDTO dto) {
        return visitaServico.atualizar(id, dto);
    }

    @PatchMapping("/{id}/fase")
    public VisitaNavioDTO alterarFase(@PathVariable Long id, @Valid @RequestBody AtualizarFaseVisitaNavioDTO dto) {
        return visitaServico.alterarFase(id, dto.fase(), dto.usuario(), dto.observacao());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        visitaServico.excluir(id);
    }

    @GetMapping("/{id}/itens")
    public List<ItemOperacaoNavioDTO> listarItens(
            @PathVariable Long id,
            @RequestParam(required = false) TipoMovimentoNavio tipoMovimento,
            @RequestParam(required = false) StatusItemCarga status
    ) {
        return itemServico.listar(id, tipoMovimento, status);
    }

    @PostMapping("/{id}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemOperacaoNavioDTO criarItem(@PathVariable Long id, @Valid @RequestBody ItemOperacaoNavioDTO dto) {
        return itemServico.criar(id, dto);
    }

    @PutMapping("/{id}/itens/{itemId}")
    public ItemOperacaoNavioDTO atualizarItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody ItemOperacaoNavioDTO dto) {
        return itemServico.atualizar(id, itemId, dto);
    }

    @PatchMapping("/{id}/itens/{itemId}/status")
    public ItemOperacaoNavioDTO alterarStatusItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody AtualizarStatusItemNavioDTO dto) {
        return itemServico.alterarStatus(id, itemId, dto.status(), dto.usuario(), dto.observacao());
    }

    @PatchMapping("/{id}/itens/{itemId}/bloqueio")
    public ItemOperacaoNavioDTO alterarBloqueioItem(@PathVariable Long id, @PathVariable Long itemId, @RequestBody BloqueioItemNavioDTO dto) {
        return itemServico.alterarBloqueio(id, itemId, dto);
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirItem(@PathVariable Long id, @PathVariable Long itemId) {
        itemServico.excluir(id, itemId);
    }

    @GetMapping("/{id}/plano-estiva")
    public PlanoEstivaNavioDTO obterPlano(@PathVariable Long id) {
        return planoServico.obter(id);
    }

    @PostMapping("/{id}/plano-estiva")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoEstivaNavioDTO criarPlano(@PathVariable Long id, @RequestBody(required = false) PlanoEstivaNavioDTO dto) {
        return planoServico.criar(id, dto);
    }

    @PutMapping("/{id}/plano-estiva/{planoId}/posicoes")
    public PlanoEstivaNavioDTO atualizarPosicoes(@PathVariable Long id, @PathVariable Long planoId, @Valid @RequestBody List<PosicaoEstivaNavioDTO> posicoes) {
        return planoServico.atualizarPosicoes(id, planoId, posicoes);
    }

    @PostMapping("/{id}/plano-estiva/{planoId}/validar")
    public ValidacaoPlanoEstivaDTO validarPlano(@PathVariable Long id, @PathVariable Long planoId) {
        return planoServico.validar(id, planoId);
    }

    @PostMapping("/{id}/plano-estiva/{planoId}/concluir")
    public PlanoEstivaNavioDTO concluirPlano(@PathVariable Long id, @PathVariable Long planoId) {
        return planoServico.concluir(id, planoId);
    }

    @PostMapping("/{id}/validacoes-estruturais")
    public ValidacaoEstruturalNavioDTO validarRestricoesEstruturais(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracaoRestricoesEstruturaisDTO configuracao
    ) {
        return validacaoEstruturalServico.validar(id, configuracao);
    }

    @GetMapping("/{id}/resumo-operacional")
    public ResumoOperacionalNavioDTO resumo(@PathVariable Long id) {
        return visitaServico.resumo(id);
    }

    @GetMapping("/{id}/eventos")
    public List<EventoVisitaNavioDTO> eventos(@PathVariable Long id) {
        return visitaServico.eventos(id);
    }

    @GetMapping(value = "/{id}/eventos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEventos(
            @PathVariable Long id,
            @RequestHeader(name = "Last-Event-ID", required = false) String ultimoEventoId
    ) {
        visitaServico.buscarEntidade(id);
        return eventoStreamingServico.assinar(id, ultimoEventoId);
    }

    @GetMapping("/{id}/control-room")
    public PainelOperacionalAvancadoDTO controlRoom(@PathVariable Long id) {
        return analiseOperacionalServico.analisar(id);
    }

    @GetMapping("/{id}/quay-monitor")
    public QuayMonitorDTO quayMonitor(@PathVariable Long id) {
        return analiseOperacionalServico.analisar(id).quayMonitor();
    }

    @PostMapping("/{id}/otimizacao-global")
    public OtimizacaoGlobalNavioPatioDTO otimizarOperacao(@PathVariable Long id) {
        return otimizacaoGlobalServico.otimizar(id);
    }

    @GetMapping("/{id}/integracao-patio")
    public ResumoIntegracaoNavioPatioDTO resumoIntegracaoPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.obterResumoIntegracao(id);
    }

    @PostMapping("/{id}/integracao-patio/reservas")
    public List<ReservaPatioNavioDTO> gerarReservasPatio(@PathVariable Long id, @RequestBody(required = false) ComandoGeracaoReservasPatioDTO dto) {
        return integracaoNavioPatioServico.gerarReservasDaVisita(id, dto);
    }

    @GetMapping("/{id}/integracao-patio/reservas")
    public List<ReservaPatioNavioDTO> listarReservasPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.listarReservasDaVisita(id);
    }

    @PostMapping("/{id}/integracao-patio/gerar-ordens")
    public ResultadoGeracaoOrdensPatioDTO gerarOrdensPatio(@PathVariable Long id, @RequestBody(required = false) ComandoGeracaoOrdensPatioDTO dto) {
        return integracaoNavioPatioServico.gerarOrdensDaVisita(id, dto);
    }

    @GetMapping("/{id}/integracao-patio/ordens")
    public List<OrdemPatioDaVisitaDTO> listarOrdensPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.listarOrdensDaVisita(id);
    }

    @PatchMapping("/{id}/integracao-patio/ordens/{ordemId}/prioridade")
    public OrdemPatioDaVisitaDTO atualizarPrioridadeOrdemPatio(@PathVariable Long id,
                                                                @PathVariable Long ordemId,
                                                                @Valid @RequestBody ComandoPrioridadeOrdemPatioDTO dto) {
        return operacaoOrdemPatioNavioServico.atualizarPrioridade(id, ordemId, dto);
    }

    @PatchMapping("/{id}/integracao-patio/ordens/{ordemId}/suspender")
    public OrdemPatioDaVisitaDTO suspenderOrdemPatio(@PathVariable Long id, @PathVariable Long ordemId) {
        return operacaoOrdemPatioNavioServico.suspender(id, ordemId);
    }

    @PatchMapping("/{id}/integracao-patio/ordens/{ordemId}/retomar")
    public OrdemPatioDaVisitaDTO retomarOrdemPatio(@PathVariable Long id, @PathVariable Long ordemId) {
        return operacaoOrdemPatioNavioServico.retomar(id, ordemId);
    }

    @GetMapping("/{id}/integracao-patio/filas")
    public List<FilaPatioDaVisitaDTO> listarFilasOperacionaisPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.listarFilasOperacionaisDaVisita(id);
    }

    @GetMapping("/{id}/integracao-patio/sem-cobertura")
    public List<OrdemPatioDaVisitaDTO> listarOrdensSemCoberturaPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.listarOrdensSemCoberturaDaVisita(id);
    }

    @GetMapping("/{id}/integracao-patio/alertas")
    public List<AlertaIntegracaoNavioPatioDTO> listarAlertasIntegracaoPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.listarAlertasIntegracao(id);
    }

    @PostMapping("/{id}/integracao-patio/sincronizar-status")
    public ResumoIntegracaoNavioPatioDTO sincronizarStatusPatio(@PathVariable Long id) {
        return integracaoNavioPatioServico.sincronizarStatus(id);
    }

    @PostMapping("/{id}/integracao-patio/replanejar")
    public ResultadoReplanejamentoPatioNavioDTO replanejarPatio(@PathVariable Long id, @RequestBody(required = false) ComandoReplanejamentoPatioNavioDTO dto) {
        return integracaoNavioPatioServico.replanejarPatioDaVisita(id, dto);
    }

    @GetMapping("/{id}/relatorio-operacional-integrado")
    public RelatorioOperacionalIntegradoDTO relatorioOperacionalIntegrado(@PathVariable Long id) {
        return integracaoNavioPatioServico.gerarRelatorioOperacionalIntegrado(id);
    }

    @GetMapping(value = "/{id}/relatorio-operacional-integrado.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> relatorioOperacionalCsv(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-operacional-visita-" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(relatorioExportacaoServico.gerarCsv(id));
    }

    @GetMapping(value = "/{id}/relatorio-operacional-integrado.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> relatorioOperacionalPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-operacional-visita-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(relatorioExportacaoServico.gerarPdf(id));
    }
}
