package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.TosContainerStatus;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ReconciliacaoBarcodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacaoBarcodeService.class);
    private static final int HORAS_PENDENCIA_ALERTA = 24;

    private final GatePassRepository gatePassRepository;
    private final ReconciliacaoBarcodeRepository reconciliacaoRepository;
    private final TosIntegrationService tosIntegrationService;

    public ReconciliacaoBarcodeService(GatePassRepository gatePassRepository,
                                       ReconciliacaoBarcodeRepository reconciliacaoRepository,
                                       TosIntegrationService tosIntegrationService) {
        this.gatePassRepository = gatePassRepository;
        this.reconciliacaoRepository = reconciliacaoRepository;
        this.tosIntegrationService = tosIntegrationService;
    }

    public List<ReconciliacaoBarcode> executarReconciliacao() {
        LOGGER.info("event=reconciliacao.iniciada timestamp={}", LocalDateTime.now());

        List<ReconciliacaoBarcode> problemasEncontrados = new ArrayList<>();

        // Verificações de reconciliação padrão
        List<GatePass> gatePassesPendentes = gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        for (GatePass gatePass : gatePassesPendentes) {
            ReconciliacaoBarcode problema = verificarTimeoutPendente(gatePass);
            if (problema != null) {
                problemasEncontrados.add(problema);
            }
        }

        List<GatePass> gatePassesEntrada = gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO);
        for (GatePass gatePass : gatePassesEntrada) {
            List<ReconciliacaoBarcode> problemas = verificarEntradaPresa(gatePass);
            problemasEncontrados.addAll(problemas);
        }

        List<GatePass> gatePassesLiberados = gatePassRepository.findByStatus(StatusGate.LIBERADO);
        for (GatePass gatePass : gatePassesLiberados) {
            List<ReconciliacaoBarcode> problemas = verificarConsistenciaBarcode(gatePass);
            problemasEncontrados.addAll(problemas);
        }

        // Anomaly Detection
        List<ReconciliacaoBarcode> anomalias = executarAnomalyDetection();
        problemasEncontrados.addAll(anomalias);

        LOGGER.info("event=reconciliacao.concluida problemas_encontrados={} timestamp={}",
                problemasEncontrados.size(), LocalDateTime.now());

        return problemasEncontrados;
    }

    private List<ReconciliacaoBarcode> executarAnomalyDetection() {
        List<ReconciliacaoBarcode> anomalias = new ArrayList<>();

        // Anomalia 1: Saída sem entrada
        anomalias.addAll(detectarSaidaSemEntrada());

        // Anomalia 2: Múltiplos containers mesma placa em 1h
        anomalias.addAll(detectarMultiplosContainersMesmaPlaca());

        // Anomalia 3: Tempo de gate > 30min
        anomalias.addAll(detectarTempoGateExcedido());

        return anomalias;
    }

    private List<ReconciliacaoBarcode> detectarSaidaSemEntrada() {
        List<ReconciliacaoBarcode> anomalias = new ArrayList<>();

        List<GatePass> gatePassesFinalizados = gatePassRepository.findByStatus(StatusGate.FINALIZADO);
        for (GatePass gatePass : gatePassesFinalizados) {
            if (gatePass.getDataEntrada() == null && gatePass.getDataSaida() != null) {
                Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                        .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                                TipoDesincroniaBarcode.SAIDA_SEM_ENTRADA);

                if (existente.isEmpty()) {
                    ReconciliacaoBarcode anomalia = new ReconciliacaoBarcode();
                    anomalia.setGatePass(gatePass);
                    anomalia.setTipoDesinconia(TipoDesincroniaBarcode.SAIDA_SEM_ENTRADA);
                    anomalia.setDescricao(String.format(
                            "Container saiu do gate sem registrar entrada. Saída: %s",
                            gatePass.getDataSaida()));
                    anomalia.setStatusLocal(StatusGate.FINALIZADO.name());
                    anomalia.setDetectadoEm(LocalDateTime.now());
                    anomalia.setAlertaEnviado(false);

                    ReconciliacaoBarcode salvo = reconciliacaoRepository.save(anomalia);
                    LOGGER.error("event=anomaly.saida_sem_entrada gatePassId={} dataSaida={} timestamp={}",
                            gatePass.getId(), gatePass.getDataSaida(), LocalDateTime.now());
                    anomalias.add(salvo);
                }
            }
        }

        return anomalias;
    }

    private List<ReconciliacaoBarcode> detectarMultiplosContainersMesmaPlaca() {
        List<ReconciliacaoBarcode> anomalias = new ArrayList<>();

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime uma_hora_atras = agora.minusHours(1);

        // Buscar todos os gate passes da última hora
        List<GatePass> gatePasses = gatePassRepository.findAll();

        Map<String, List<GatePass>> porPlaca = new HashMap<>();
        for (GatePass gatePass : gatePasses) {
            if (gatePass.getDataEntrada() != null &&
                gatePass.getDataEntrada().isAfter(uma_hora_atras) &&
                gatePass.getDataEntrada().isBefore(agora)) {

                String placa = gatePass.getAgendamento().getVeiculo().getPlaca();
                porPlaca.computeIfAbsent(placa, k -> new ArrayList<>()).add(gatePass);
            }
        }

        // Detectar placas com múltiplos containers
        for (Map.Entry<String, List<GatePass>> entry : porPlaca.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (GatePass gatePass : entry.getValue()) {
                    Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                            .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                                    TipoDesincroniaBarcode.MULTIPLOS_CONTAINERS_PLACA);

                    if (existente.isEmpty()) {
                        ReconciliacaoBarcode anomalia = new ReconciliacaoBarcode();
                        anomalia.setGatePass(gatePass);
                        anomalia.setTipoDesinconia(TipoDesincroniaBarcode.MULTIPLOS_CONTAINERS_PLACA);
                        anomalia.setDescricao(String.format(
                                "Placa %s registrou %d containers em 1 hora (suspeito de fraude/roubo)",
                                entry.getKey(), entry.getValue().size()));
                        anomalia.setStatusLocal(gatePass.getStatus().name());
                        anomalia.setDetectadoEm(agora);
                        anomalia.setAlertaEnviado(false);

                        ReconciliacaoBarcode salvo = reconciliacaoRepository.save(anomalia);
                        LOGGER.warn("event=anomaly.multiplos_containers placa={} quantidade={} timestamp={}",
                                entry.getKey(), entry.getValue().size(), agora);
                        anomalias.add(salvo);
                    }
                }
            }
        }

        return anomalias;
    }

    private List<ReconciliacaoBarcode> detectarTempoGateExcedido() {
        List<ReconciliacaoBarcode> anomalias = new ArrayList<>();

        List<GatePass> gatePasses = gatePassRepository.findAll();
        for (GatePass gatePass : gatePasses) {
            if (gatePass.getDataEntrada() != null && gatePass.getDataSaida() == null) {
                LocalDateTime agora = LocalDateTime.now();
                Duration tempo = Duration.between(gatePass.getDataEntrada(), agora);
                long minutos = tempo.toMinutes();

                if (minutos > 30) {
                    Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                            .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                                    TipoDesincroniaBarcode.TEMPO_GATE_EXCEDIDO);

                    if (existente.isEmpty()) {
                        ReconciliacaoBarcode anomalia = new ReconciliacaoBarcode();
                        anomalia.setGatePass(gatePass);
                        anomalia.setTipoDesinconia(TipoDesincroniaBarcode.TEMPO_GATE_EXCEDIDO);
                        anomalia.setDescricao(String.format(
                                "Container no gate há %d minutos (limite: 30min). Possível avaria ou obstrução.",
                                minutos));
                        anomalia.setStatusLocal(gatePass.getStatus().name());
                        anomalia.setTempoPendenciaHoras((int) (minutos / 60));
                        anomalia.setDetectadoEm(agora);
                        anomalia.setAlertaEnviado(false);

                        ReconciliacaoBarcode salvo = reconciliacaoRepository.save(anomalia);
                        LOGGER.warn("event=anomaly.tempo_gate_excedido gatePassId={} minutos={} timestamp={}",
                                gatePass.getId(), minutos, agora);
                        anomalias.add(salvo);
                    }
                }
            }
        }

        return anomalias;
    }

    private ReconciliacaoBarcode verificarTimeoutPendente(GatePass gatePass) {
        LocalDateTime agora = LocalDateTime.now();
        Duration tempoPendencia = Duration.between(gatePass.getDataEntrada(), agora);
        long minutos = tempoPendencia.toMinutes();

        if (minutos > 30) {
            Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                    .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                            TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO);

            if (existente.isEmpty()) {
                ReconciliacaoBarcode problema = new ReconciliacaoBarcode();
                problema.setGatePass(gatePass);
                problema.setTipoDesinconia(TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO);
                problema.setDescricao(String.format(
                        "Container aguardando confirmação de barcode há %d minutos", minutos));
                problema.setStatusLocal(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE.name());
                problema.setTempoPendenciaHoras((int) (minutos / 60));
                problema.setDetectadoEm(agora);
                problema.setAlertaEnviado(false);

                ReconciliacaoBarcode salvo = reconciliacaoRepository.save(problema);
                LOGGER.warn("event=reconciliacao.barcode_pendente gatePassId={} minutos={} timestamp={}",
                        gatePass.getId(), minutos, agora);
                return salvo;
            }
        }
        return null;
    }

    private List<ReconciliacaoBarcode> verificarEntradaPresa(GatePass gatePass) {
        List<ReconciliacaoBarcode> problemas = new ArrayList<>();

        if (gatePass.getDataEntrada() == null || gatePass.getDataSaida() != null) {
            return problemas;
        }

        LocalDateTime agora = LocalDateTime.now();
        Duration tempoEntrada = Duration.between(gatePass.getDataEntrada(), agora);
        long horas = tempoEntrada.toHours();

        if (horas >= HORAS_PENDENCIA_ALERTA) {
            Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                    .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                            TipoDesincroniaBarcode.ENTRADA_SEM_SAIDA_24H);

            if (existente.isEmpty()) {
                ReconciliacaoBarcode problema = new ReconciliacaoBarcode();
                problema.setGatePass(gatePass);
                problema.setTipoDesinconia(TipoDesincroniaBarcode.ENTRADA_SEM_SAIDA_24H);
                problema.setDescricao(String.format(
                        "Container na entrada há %d horas sem registrar saída", horas));
                problema.setStatusLocal(StatusGate.EM_PROCESSAMENTO.name());
                problema.setTempoPendenciaHoras((int) horas);
                problema.setDetectadoEm(agora);
                problema.setAlertaEnviado(false);

                ReconciliacaoBarcode salvo = reconciliacaoRepository.save(problema);
                LOGGER.error("event=reconciliacao.container_preso gatePassId={} horas={} timestamp={}",
                        gatePass.getId(), horas, agora);
                problemas.add(salvo);
            }
        }

        return problemas;
    }

    private List<ReconciliacaoBarcode> verificarConsistenciaBarcode(GatePass gatePass) {
        List<ReconciliacaoBarcode> problemas = new ArrayList<>();

        try {
            if (!StringUtils.hasText(gatePass.getAgendamento().getCodigo())) {
                return problemas;
            }

            TosContainerStatus statusTos = tosIntegrationService.obterStatusContainer(
                    gatePass.getAgendamento().getCodigo());

            if (statusTos != null && gatePass.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.CONFIRMADO) {
                boolean mismatch = !statusTos.getContainerNumber().equals(gatePass.getCodigoBarcode());

                if (mismatch) {
                    Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                            .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                                    TipoDesincroniaBarcode.BARCODE_MISMATCH);

                    if (existente.isEmpty()) {
                        ReconciliacaoBarcode problema = new ReconciliacaoBarcode();
                        problema.setGatePass(gatePass);
                        problema.setTipoDesinconia(TipoDesincroniaBarcode.BARCODE_MISMATCH);
                        problema.setBarcodeEsperado(statusTos.getContainerNumber());
                        problema.setBarcodeRecebido(gatePass.getCodigoBarcode());
                        problema.setStatusTos(statusTos.getStatus());
                        problema.setStatusLocal(StatusGate.LIBERADO.name());
                        problema.setDescricao(String.format(
                                "Barcode confirmado (%s) não corresponde ao TOS (%s)",
                                gatePass.getCodigoBarcode(), statusTos.getContainerNumber()));
                        problema.setDetectadoEm(LocalDateTime.now());
                        problema.setAlertaEnviado(false);

                        ReconciliacaoBarcode salvo = reconciliacaoRepository.save(problema);
                        LOGGER.error("event=reconciliacao.barcode_mismatch gatePassId={} esperado={} recebido={} timestamp={}",
                                gatePass.getId(), statusTos.getContainerNumber(),
                                gatePass.getCodigoBarcode(), LocalDateTime.now());
                        problemas.add(salvo);
                    }
                }
            }

            if (!statusTos.isGateLiberado() && gatePass.getStatus() == StatusGate.LIBERADO) {
                Optional<ReconciliacaoBarcode> existente = reconciliacaoRepository
                        .findByGatePassIdAndTipoDesinconia(gatePass.getId(),
                                TipoDesincroniaBarcode.STATUS_INCONSISTENTE);

                if (existente.isEmpty()) {
                    ReconciliacaoBarcode problema = new ReconciliacaoBarcode();
                    problema.setGatePass(gatePass);
                    problema.setTipoDesinconia(TipoDesincroniaBarcode.STATUS_INCONSISTENTE);
                    problema.setStatusTos(statusTos.getStatus());
                    problema.setStatusLocal(StatusGate.LIBERADO.name());
                    problema.setDescricao(String.format(
                            "Container liberado localmente mas bloqueado no TOS: %s",
                            statusTos.getMotivoRestricao()));
                    problema.setDetectadoEm(LocalDateTime.now());
                    problema.setAlertaEnviado(false);

                    ReconciliacaoBarcode salvo = reconciliacaoRepository.save(problema);
                    LOGGER.warn("event=reconciliacao.status_inconsistente gatePassId={} tos={} local={} timestamp={}",
                            gatePass.getId(), statusTos.getStatus(), StatusGate.LIBERADO.name(), LocalDateTime.now());
                    problemas.add(salvo);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("event=reconciliacao.erro_verificacao gatePassId={} cause={} timestamp={}",
                    gatePass.getId(), ex.getMessage(), LocalDateTime.now());
        }

        return problemas;
    }

    public void resolverDesinconia(Long reconciliacaoId, String resolucao) {
        ReconciliacaoBarcode reconciliacao = reconciliacaoRepository.findById(reconciliacaoId)
                .orElseThrow(() -> new RuntimeException("Reconciliação não encontrada"));

        reconciliacao.setResolvidoEm(LocalDateTime.now());
        reconciliacao.setResolucao(resolucao);
        reconciliacaoRepository.save(reconciliacao);

        LOGGER.info("event=reconciliacao.resolvida id={} tipo={} resolucao={} timestamp={}",
                reconciliacaoId, reconciliacao.getTipoDesinconia(), resolucao, LocalDateTime.now());
    }

    public List<ReconciliacaoBarcode> listarNaoResolvidas() {
        return reconciliacaoRepository.findByResolvidoEmIsNull();
    }

    public List<ReconciliacaoBarcode> listarPorTipo(TipoDesincroniaBarcode tipo) {
        return reconciliacaoRepository.findByTipoDesinconia(tipo);
    }
}
