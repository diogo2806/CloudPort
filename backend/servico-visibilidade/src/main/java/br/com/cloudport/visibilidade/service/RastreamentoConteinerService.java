package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO;
import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RastreamentoConteinerService {

    private final ConteinerLocalizacaoRepository localizacaoRepository;
    private final HistoricoMovimentoRepository historicoRepository;

    public RastreamentoConteinerService(ConteinerLocalizacaoRepository localizacaoRepository,
                                        HistoricoMovimentoRepository historicoRepository) {
        this.localizacaoRepository = localizacaoRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public ConteinerRastreamentoDTO rastrearContainer(String containerId) {
        ConteinerLocalizacao localizacao = localizacaoRepository.findByContainerId(containerId)
                .orElseThrow(() -> new IllegalArgumentException("Container nao encontrado: " + containerId));

        List<HistoricoMovimento> historico = historicoRepository.findByContainerIdOrderByTimestampDesc(containerId);

        ConteinerRastreamentoDTO dto = new ConteinerRastreamentoDTO();
        dto.setContainerId(localizacao.getContainerId());
        dto.setStatusAtual(localizacao.getStatusAtual());
        dto.setLocalizacaoAtual(mapearLocalizacaoAtual(localizacao));
        dto.setProximoDestino(mapearProximoDestino(localizacao));
        dto.setRotaCompleta(historico.stream().map(this::mapearRota).collect(Collectors.toList()));
        dto.setMetricas(montarMetricas(localizacao, historico));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<HistoricoMovimento> obterHistorico(String containerId) {
        return historicoRepository.findByContainerIdOrderByTimestampDesc(containerId);
    }

    @Transactional(readOnly = true)
    public List<ConteinerLocalizacao> buscarContainers(String containerId,
                                                       String status,
                                                       String zona,
                                                       String navioDestino) {
        return localizacaoRepository.findAll().stream()
                .filter(conteiner -> filtrarCampo(conteiner.getContainerId(), containerId))
                .filter(conteiner -> filtrarCampo(conteiner.getStatusAtual(), status))
                .filter(conteiner -> filtrarCampo(conteiner.getZona(), zona))
                .filter(conteiner -> filtrarCampo(conteiner.getNavioDestinoId(), navioDestino))
                .sorted(Comparator.comparing(ConteinerLocalizacao::getDataAtualizacao,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    private ConteinerRastreamentoDTO.LocalizacaoDTO mapearLocalizacaoAtual(ConteinerLocalizacao localizacao) {
        ConteinerRastreamentoDTO.LocalizacaoDTO dto = new ConteinerRastreamentoDTO.LocalizacaoDTO();
        dto.setTipo("ATUAL");
        dto.setZona(localizacao.getZona());
        dto.setPosicao(localizacao.getPosicao());

        if (localizacao.getLatitude() != null || localizacao.getLongitude() != null) {
            ConteinerRastreamentoDTO.CoordenadasDTO coordenadas = new ConteinerRastreamentoDTO.CoordenadasDTO();
            coordenadas.setLatitude(localizacao.getLatitude());
            coordenadas.setLongitude(localizacao.getLongitude());
            dto.setCoordenadas(coordenadas);
        }

        dto.setDataAtualizacao(localizacao.getDataAtualizacao());
        return dto;
    }

    private ConteinerRastreamentoDTO.ProximoDestinoDTO mapearProximoDestino(ConteinerLocalizacao localizacao) {
        if (!StringUtils.hasText(localizacao.getNavioDestinoId())) {
            return null;
        }

        ConteinerRastreamentoDTO.ProximoDestinoDTO dto = new ConteinerRastreamentoDTO.ProximoDestinoDTO();
        dto.setTipo("NAVIO");
        dto.setId(localizacao.getNavioDestinoId());
        dto.setBerco(null);
        dto.setEstimadoParaSaida(null);
        return dto;
    }

    private ConteinerRastreamentoDTO.RotaDTO mapearRota(HistoricoMovimento movimento) {
        ConteinerRastreamentoDTO.RotaDTO dto = new ConteinerRastreamentoDTO.RotaDTO();
        dto.setSequencia(movimento.getId() == null ? null : movimento.getId().intValue());
        dto.setLocal(movimento.getLocalizacao());
        dto.setTimestamp(movimento.getTimestamp());
        dto.setStatus(movimento.getTipo());
        return dto;
    }

    private ConteinerRastreamentoDTO.MetricasDTO montarMetricas(ConteinerLocalizacao localizacao,
                                                                 List<HistoricoMovimento> historico) {
        ConteinerRastreamentoDTO.MetricasDTO metricas = new ConteinerRastreamentoDTO.MetricasDTO();
        metricas.setTempoNoYard(calcularTempoNoYard(localizacao, historico));
        metricas.setDataPrevisaoSaida(null);
        return metricas;
    }

    private String calcularTempoNoYard(ConteinerLocalizacao localizacao,
                                       List<HistoricoMovimento> historico) {
        if (!MovimentoConteinerService.STATUS_NO_YARD.equalsIgnoreCase(localizacao.getStatusAtual())) {
            return null;
        }

        LocalDateTime entradaNoYard = historico.stream()
                .filter(this::iniciaPermanenciaNoYard)
                .map(HistoricoMovimento::getTimestamp)
                .filter(timestamp -> timestamp != null)
                .max(LocalDateTime::compareTo)
                .orElse(localizacao.getDataAtualizacao());

        if (entradaNoYard == null) {
            return null;
        }

        long minutos = Math.max(0L, Duration.between(entradaNoYard, LocalDateTime.now()).toMinutes());
        long horas = minutos / 60;
        long minutosRestantes = minutos % 60;
        return horas + "h " + minutosRestantes + "min";
    }

    private boolean iniciaPermanenciaNoYard(HistoricoMovimento movimento) {
        return MovimentoConteinerService.TIPO_ENTRADA_GATE.equalsIgnoreCase(movimento.getTipo())
                || MovimentoConteinerService.TIPO_ARMAZENAGEM_YARD.equalsIgnoreCase(movimento.getTipo());
    }

    private boolean filtrarCampo(String valor, String filtro) {
        if (!StringUtils.hasText(filtro)) {
            return true;
        }
        return StringUtils.hasText(valor)
                && valor.toLowerCase(Locale.ROOT).contains(filtro.toLowerCase(Locale.ROOT));
    }
}
