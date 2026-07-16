package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.entity.StatusNavio;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StatusNavioService {

    private static final String STATUS_ANCORANDO = "ancorando";

    private final StatusNavioRepository statusNavioRepository;
    private final AlertaRepository alertaRepository;

    public StatusNavioService(StatusNavioRepository statusNavioRepository,
                              AlertaRepository alertaRepository) {
        this.statusNavioRepository = statusNavioRepository;
        this.alertaRepository = alertaRepository;
    }

    @Transactional(readOnly = true)
    public StatusNavioDTO getStatusNavio(String navioId) {
        StatusNavio navio = statusNavioRepository.findByNavioId(navioId)
                .orElseThrow(() -> new IllegalArgumentException("Navio nao encontrado: " + navioId));
        return mapToDTO(navio);
    }

    @Transactional(readOnly = true)
    public List<StatusNavioDTO> listarNaviosEmOperacao() {
        return statusNavioRepository.findByStatusOperacional("operando").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void atualizarStatusNavio(String navioId, String status, String berco) {
        if (!StringUtils.hasText(navioId)) {
            throw new IllegalArgumentException("navioId e obrigatorio para atualizar o status do navio.");
        }

        StatusNavio navio = statusNavioRepository.findByNavioId(navioId)
                .orElseGet(() -> criarStatusNavio(navioId));

        if (StringUtils.hasText(status)) {
            navio.setStatusOperacional(status.trim());
            if (STATUS_ANCORANDO.equalsIgnoreCase(status.trim()) && navio.getChegadaReal() == null) {
                navio.setChegadaReal(LocalDateTime.now());
            }
        }
        if (StringUtils.hasText(berco)) {
            navio.setBercoAlocado(berco.trim());
        }

        navio.setDataAtualizacao(LocalDateTime.now());
        statusNavioRepository.save(navio);
    }

    private StatusNavio criarStatusNavio(String navioId) {
        StatusNavio navio = new StatusNavio();
        navio.setNavioId(navioId.trim());
        return navio;
    }

    private StatusNavioDTO mapToDTO(StatusNavio entity) {
        StatusNavioDTO dto = new StatusNavioDTO();
        dto.setNavioId(entity.getNavioId());
        dto.setNomeNavio(entity.getNomeNavio());
        dto.setStatusOperacional(entity.getStatusOperacional());

        StatusNavioDTO.EtaDTO eta = new StatusNavioDTO.EtaDTO();
        eta.setEstimado(entity.getEtaEstimado());
        eta.setChegadaReal(entity.getChegadaReal());
        eta.setAtraso(entity.getAtrasoMinutos());
        dto.setEtaCurrent(eta);

        if (StringUtils.hasText(entity.getBercoAlocado())) {
            StatusNavioDTO.BercoDTO berco = new StatusNavioDTO.BercoDTO();
            berco.setNumero(entity.getBercoAlocado());
            berco.setDataInicio(entity.getChegadaReal());
            berco.setDataPrevistaSaida(null);
            dto.setBercoAlocado(berco);
        }

        StatusNavioDTO.OperacoesDTO operacoes = new StatusNavioDTO.OperacoesDTO();
        operacoes.setPorcentagemCompleta(entity.getPorcentagemCompleta());
        dto.setOperacoesEmAndamento(operacoes);

        dto.setEquipamentosAlocados(Collections.emptyList());
        dto.setAlertasNavio(mapearAlertasAtivos(entity.getNavioId()));
        dto.setTimeline(criarTimeline(entity));
        return dto;
    }

    private List<StatusNavioDTO.AlertaResumoDTO> mapearAlertasAtivos(String entidadeId) {
        return alertaRepository.findByEntidadeIdAndStatus(entidadeId, "ativo").stream()
                .map(this::mapearAlertaResumo)
                .collect(Collectors.toList());
    }

    private StatusNavioDTO.AlertaResumoDTO mapearAlertaResumo(Alerta alerta) {
        StatusNavioDTO.AlertaResumoDTO dto = new StatusNavioDTO.AlertaResumoDTO();
        dto.setId(alerta.getId());
        dto.setTipo(alerta.getTipo());
        dto.setSeveridade(alerta.getSeveridade());
        dto.setDescricao(alerta.getDescricao());
        dto.setDataGerada(alerta.getDataGerada());
        return dto;
    }

    private List<StatusNavioDTO.TimelineDTO> criarTimeline(StatusNavio navio) {
        List<StatusNavioDTO.TimelineDTO> timeline = new ArrayList<>();
        if (navio.getEtaEstimado() != null) {
            timeline.add(timeline("ETA estimado", navio.getEtaEstimado()));
        }
        if (navio.getChegadaReal() != null) {
            timeline.add(timeline("Chegada real", navio.getChegadaReal()));
        }
        if (StringUtils.hasText(navio.getBercoAlocado()) && navio.getDataAtualizacao() != null) {
            timeline.add(timeline("Berco alocado", navio.getDataAtualizacao()));
        }
        if (StringUtils.hasText(navio.getStatusOperacional()) && navio.getDataAtualizacao() != null) {
            timeline.add(timeline("Status operacional", navio.getDataAtualizacao()));
        }
        return timeline;
    }

    private StatusNavioDTO.TimelineDTO timeline(String evento, LocalDateTime tempo) {
        StatusNavioDTO.TimelineDTO dto = new StatusNavioDTO.TimelineDTO();
        dto.setEvento(evento);
        dto.setTempo(tempo);
        return dto;
    }
}
