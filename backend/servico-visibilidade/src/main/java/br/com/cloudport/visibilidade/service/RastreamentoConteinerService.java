package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.ConteinerRastreamentoDTO;
import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RastreamentoConteinerService {

    @Autowired
    private ConteinerLocalizacaoRepository localizacaoRepository;

    @Autowired
    private HistoricoMovimentoRepository historicoRepository;

    public ConteinerRastreamentoDTO rastrearContainer(String containerId) {
        ConteinerLocalizacao localizacao = localizacaoRepository.findByContainerId(containerId)
                .orElseThrow(() -> new RuntimeException("Container não encontrado: " + containerId));

        List<HistoricoMovimento> historico = historicoRepository.findByContainerIdOrderByTimestampDesc(containerId);

        // TODO: Montar DTO completo com localização, próximo destino, rota e métricas
        ConteinerRastreamentoDTO dto = new ConteinerRastreamentoDTO();
        dto.setContainerId(containerId);
        dto.setStatusAtual(localizacao.getStatusAtual());

        return dto;
    }

    public List<HistoricoMovimento> obterHistorico(String containerId) {
        return historicoRepository.findByContainerIdOrderByTimestampDesc(containerId);
    }

    // TODO: Implementar lógica de busca com filtros
    public List<ConteinerLocalizacao> buscarContainers(String containerId, String status, String zona, String navioDestino) {
        // Implementar filtros dinâmicos
        return localizacaoRepository.findAll();
    }
}