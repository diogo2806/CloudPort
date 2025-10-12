package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.dto.dashboard.DashboardFiltroDTO;
import br.com.cloudport.servicogate.dto.relatorio.FormatoExportacao;
import br.com.cloudport.servicogate.dto.relatorio.RelatorioAgendamentoDTO;
import br.com.cloudport.servicogate.dto.relatorio.RelatorioResponseDTO;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.service.DashboardService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/relatorios")
public class RelatorioController {

    private final DashboardService dashboardService;

    public RelatorioController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public RelatorioResponseDTO consultar(
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(value = "transportadoraId", required = false) Long transportadoraId,
            @RequestParam(value = "tipoOperacao", required = false) String tipoOperacao
    ) {
        DashboardFiltroDTO filtro = construirFiltro(inicio, fim, transportadoraId, tipoOperacao);
        List<RelatorioAgendamentoDTO> agendamentos = dashboardService.buscarRelatorio(filtro);
        return new RelatorioResponseDTO(dashboardService.obterResumo(filtro), agendamentos);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(value = "transportadoraId", required = false) Long transportadoraId,
            @RequestParam(value = "tipoOperacao", required = false) String tipoOperacao,
            @RequestParam(value = "formato", required = false) String formato
    ) {
        DashboardFiltroDTO filtro = construirFiltro(inicio, fim, transportadoraId, tipoOperacao);
        FormatoExportacao formatoExportacao = FormatoExportacao.from(formato);
        byte[] arquivo = dashboardService.exportarRelatorio(filtro, formatoExportacao);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(formatoExportacao.getMediaType());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("relatorio-gate." + formatoExportacao.getExtensao())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(arquivo);
    }

    private DashboardFiltroDTO construirFiltro(LocalDateTime inicio, LocalDateTime fim,
                                               Long transportadoraId, String tipoOperacao) {
        DashboardFiltroDTO filtro = new DashboardFiltroDTO();
        filtro.setInicio(inicio);
        filtro.setFim(fim);
        filtro.setTransportadoraId(transportadoraId);
        if (tipoOperacao != null && !tipoOperacao.isBlank()) {
            filtro.setTipoOperacao(TipoOperacao.valueOf(tipoOperacao.toUpperCase()));
        }
        return filtro;
    }
}
