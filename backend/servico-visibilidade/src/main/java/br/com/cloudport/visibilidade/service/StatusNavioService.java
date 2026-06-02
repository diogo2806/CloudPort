package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.AlertaDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatusNavioService {

    @Autowired
    private StatusNavioRepository statusNavioRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    public StatusNavioDTO getStatusNavio(String navioId) {
        StatusNavio navio = statusNavioRepository.findByNavioId(navioId)
                .orElseThrow(() -> new IllegalArgumentException("Navio nao encontrado: " + navioId));
        return mapToDTO(navio);
    }

    public List<StatusNavioDTO> listarNaviosEmOperacao() {
        return statusNavioRepository.findByStatusOperacional("operando").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void atualizarStatusNavio(String navioId, String status, String berco) {
        statusNavioRepository.findByNavioId(navioId).ifPresent(navio -> {
            navio.setStatusOperacional(status);
            if (berco != null) {
                navio.setBercoAlocado(berco);
            }
            navio.setDataAtualizacao(LocalDateTime.now());
            statusNavioRepository.save(navio);
        });
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
            berco.setDataPrevistaSaida(entity.getEtaEstimado());
            dto.setBercoAlocado(berco);
        }

        StatusNavioDTO.OperacoesDTO operacoes = new StatusNavioDTO.OperacoesDTO();
        operacoes.setPorcentagemCompleta(entity.getPorcentagemCompleta());
        int totalConteineres = 1000;
        int concluido = entity.getPorcentagemCompleta() == null
                ? 0
                : (int) Math.round(totalConteineres * (entity.getPorcentagemCompleta() / 100.0));
        operacoes.setConteineresADescarregar(totalConteineres);
        operacoes.setConteineresDescarregados(concluido);
        operacoes.setVelocidadeMov(28d);
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
        if (StringUtils.hasText(navio.getBercoAlocado())) {
            timeline.add(timeline("Berco alocado", navio.getDataAtualizacao()));
        }
        timeline.add(timeline("Status operacional", navio.getDataAtualizacao()));
        return timeline;
    }

    private StatusNavioDTO.TimelineDTO timeline(String evento, LocalDateTime tempo) {
        StatusNavioDTO.TimelineDTO dto = new StatusNavioDTO.TimelineDTO();
        dto.setEvento(evento);
        dto.setTempo(tempo);
        return dto;
    }
}
