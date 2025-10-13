package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.dto.dashboard.DashboardFiltroDTO;
import br.com.cloudport.servicogate.dto.dashboard.DashboardResumoDTO;
import br.com.cloudport.servicogate.dto.dashboard.OcupacaoPorHoraDTO;
import br.com.cloudport.servicogate.dto.dashboard.TempoMedioPermanenciaDTO;
import br.com.cloudport.servicogate.dto.relatorio.FormatoExportacao;
import br.com.cloudport.servicogate.dto.relatorio.RelatorioAgendamentoDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.repository.AgendamentoRepository;
import br.com.cloudport.servicogate.repository.projection.DashboardMetricsProjection;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TOLERANCIA_PONTUALIDADE_MINUTOS = 15;
    private static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

    private final AgendamentoRepository agendamentoRepository;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public DashboardService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    public DashboardResumoDTO obterResumo(DashboardFiltroDTO filtro) {
        DashboardFiltroDTO filtroNormalizado = normalizarFiltro(filtro);

        DashboardMetricsProjection projection = agendamentoRepository.calcularMetricasDashboard(
                filtroNormalizado.getInicio(),
                filtroNormalizado.getFim(),
                filtroNormalizado.getTransportadoraId(),
                filtroNormalizado.getTipoOperacao() != null ? filtroNormalizado.getTipoOperacao().name() : null,
                TOLERANCIA_PONTUALIDADE_MINUTOS
        );

        long total = Optional.ofNullable(projection.getTotalAgendamentos()).orElse(0L);
        long pontuais = Optional.ofNullable(projection.getPontuais()).orElse(0L);
        long noShow = Optional.ofNullable(projection.getNoShow()).orElse(0L);
        double turnaround = Optional.ofNullable(projection.getTurnaroundMedio()).orElse(0D);
        double ocupacao = Optional.ofNullable(projection.getOcupacaoSlots()).orElse(0D);

        List<Agendamento> agendamentosFiltrados = buscarAgendamentos(filtroNormalizado);

        DashboardResumoDTO resumo = new DashboardResumoDTO();
        resumo.setTotalAgendamentos(total);
        resumo.setPercentualPontualidade(total > 0 ? (pontuais * 100.0) / total : 0D);
        resumo.setPercentualNoShow(total > 0 ? (noShow * 100.0) / total : 0D);
        resumo.setPercentualOcupacaoSlots(ocupacao * 100.0);
        resumo.setTempoMedioTurnaroundMinutos(turnaround);
        resumo.setOcupacaoPorHora(calcularOcupacaoPorHora(agendamentosFiltrados));
        resumo.setTurnaroundPorDia(calcularTurnaroundPorDia(agendamentosFiltrados));
        return resumo;
    }

    public List<RelatorioAgendamentoDTO> buscarRelatorio(DashboardFiltroDTO filtro) {
        List<Agendamento> agendamentos = buscarAgendamentos(normalizarFiltro(filtro));
        if (CollectionUtils.isEmpty(agendamentos)) {
            return Collections.emptyList();
        }
        return agendamentos.stream()
                .map(RelatorioAgendamentoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public byte[] exportarRelatorio(DashboardFiltroDTO filtro, FormatoExportacao formato) {
        List<RelatorioAgendamentoDTO> linhas = buscarRelatorio(filtro);
        if (formato == FormatoExportacao.EXCEL) {
            return gerarPlanilhaExcel(linhas);
        }
        return gerarCsv(linhas);
    }

    public SseEmitter registrarAssinante() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(throwable -> emitters.remove(emitter));
        enviarSnapshotInicial(emitter);
        return emitter;
    }

    public void publicarResumo(DashboardFiltroDTO filtro) {
        DashboardResumoDTO resumo = obterResumo(filtro);
        publicarAtualizacao(resumo);
    }

    public void publicarResumoGeral() {
        publicarResumo(null);
    }

    private void publicarAtualizacao(DashboardResumoDTO resumo) {
        if (emitters.isEmpty()) {
            return;
        }
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("dashboard-atualizado")
                        .data(resumo));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
                emitters.remove(emitter);
            }
        });
    }

    private DashboardFiltroDTO normalizarFiltro(DashboardFiltroDTO filtro) {
        return filtro != null ? filtro : new DashboardFiltroDTO();
    }

    private List<Agendamento> buscarAgendamentos(DashboardFiltroDTO filtro) {
        return agendamentoRepository.buscarRelatorio(
                filtro.getInicio(),
                filtro.getFim(),
                filtro.getTransportadoraId(),
                filtro.getTipoOperacao()
        );
    }

    private void enviarSnapshotInicial(SseEmitter emitter) {
        try {
            DashboardResumoDTO resumo = obterResumo(null);
            emitter.send(SseEmitter.event()
                    .name("dashboard-atualizado")
                    .data(resumo));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            emitters.remove(emitter);
        }
    }

    private List<OcupacaoPorHoraDTO> calcularOcupacaoPorHora(List<Agendamento> agendamentos) {
        if (CollectionUtils.isEmpty(agendamentos)) {
            return Collections.emptyList();
        }
        return agendamentos.stream()
                .collect(Collectors.groupingBy(agendamento -> agendamento.getJanelaAtendimento().getHoraInicio()))
                .entrySet()
                .stream()
                .map(entry -> {
                    long total = entry.getValue().stream()
                            .filter(agendamento -> agendamento.getStatus() != null
                                    && agendamento.getStatus() != br.com.cloudport.servicogate.model.enums.StatusAgendamento.CANCELADO)
                            .count();
                    Integer capacidade = Optional.ofNullable(entry.getValue().get(0).getJanelaAtendimento().getCapacidade())
                            .orElse(0);
                    return new OcupacaoPorHoraDTO(entry.getKey(), total, capacidade);
                })
                .sorted((a, b) -> a.getHoraInicio().compareTo(b.getHoraInicio()))
                .collect(Collectors.toList());
    }

    private List<TempoMedioPermanenciaDTO> calcularTurnaroundPorDia(List<Agendamento> agendamentos) {
        if (CollectionUtils.isEmpty(agendamentos)) {
            return Collections.emptyList();
        }
        return agendamentos.stream()
                .filter(agendamento -> Objects.nonNull(agendamento.getHorarioRealChegada())
                        && Objects.nonNull(agendamento.getHorarioRealSaida()))
                .collect(Collectors.groupingBy(agendamento -> agendamento.getHorarioRealSaida().toLocalDate()))
                .entrySet()
                .stream()
                .map(entry -> {
                    double media = entry.getValue().stream()
                            .mapToDouble(agendamento -> Duration.between(
                                    agendamento.getHorarioRealChegada(),
                                    agendamento.getHorarioRealSaida()
                            ).toMinutes())
                            .average()
                            .orElse(0D);
                    return new TempoMedioPermanenciaDTO(entry.getKey(), media);
                })
                .sorted((a, b) -> a.getDia().compareTo(b.getDia()))
                .collect(Collectors.toList());
    }

    private byte[] gerarCsv(List<RelatorioAgendamentoDTO> linhas) {
        try (StringWriter out = new StringWriter(); CSVWriter writer = new CSVWriter(out)) {
            writer.writeNext(new String[]{
                    "Código",
                    "Tipo de Operação",
                    "Status",
                    "Transportadora",
                    "Previsto Chegada",
                    "Real Chegada",
                    "Previsto Saída",
                    "Real Saída"
            });

            for (RelatorioAgendamentoDTO dto : linhas) {
                writer.writeNext(new String[]{
                        dto.getCodigo(),
                        Optional.ofNullable(dto.getTipoOperacao()).map(TipoOperacao::name).orElse(""),
                        Optional.ofNullable(dto.getStatus()).map(Enum::name).orElse(""),
                        Optional.ofNullable(dto.getTransportadora()).orElse(""),
                        formatarData(dto.getHorarioPrevistoChegada()),
                        formatarData(dto.getHorarioRealChegada()),
                        formatarData(dto.getHorarioPrevistoSaida()),
                        formatarData(dto.getHorarioRealSaida())
                });
            }
            writer.flush();
            return out.toString().getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao gerar CSV de relatórios", ex);
        }
    }

    private byte[] gerarPlanilhaExcel(List<RelatorioAgendamentoDTO> linhas) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Relatório Gate");
            int rowIndex = 0;
            Row header = sheet.createRow(rowIndex++);
            String[] cabecalho = {
                    "Código",
                    "Tipo de Operação",
                    "Status",
                    "Transportadora",
                    "Previsto Chegada",
                    "Real Chegada",
                    "Previsto Saída",
                    "Real Saída"
            };
            for (int i = 0; i < cabecalho.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cabecalho[i]);
            }

            for (RelatorioAgendamentoDTO dto : linhas) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(dto.getCodigo());
                row.createCell(col++).setCellValue(Optional.ofNullable(dto.getTipoOperacao()).map(TipoOperacao::name).orElse(""));
                row.createCell(col++).setCellValue(Optional.ofNullable(dto.getStatus()).map(Enum::name).orElse(""));
                row.createCell(col++).setCellValue(Optional.ofNullable(dto.getTransportadora()).orElse(""));
                row.createCell(col++).setCellValue(formatarData(dto.getHorarioPrevistoChegada()));
                row.createCell(col++).setCellValue(formatarData(dto.getHorarioRealChegada()));
                row.createCell(col++).setCellValue(formatarData(dto.getHorarioPrevistoSaida()));
                row.createCell(col++).setCellValue(formatarData(dto.getHorarioRealSaida()));
            }

            for (int i = 0; i < cabecalho.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao gerar planilha Excel do relatório", ex);
        }
    }

    private String formatarData(LocalDateTime data) {
        return data != null ? data.toString() : "";
    }
}
