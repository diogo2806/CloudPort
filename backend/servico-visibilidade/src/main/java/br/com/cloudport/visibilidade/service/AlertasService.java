package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertasService {

    @Autowired
    private AlertaRepository alertaRepository;

    public List<Alerta> listarAlertasAtivos() {
        return alertaRepository.findByStatusOrderByDataGeradaDesc("ativo");
    }

    public Page<Alerta> buscarAlertasFiltrados(List<String> severidades, List<String> tipos, String status, Pageable pageable) {
        return alertaRepository.findBySeveridadeInAndTipoInAndStatus(severidades, tipos, status, pageable);
    }

    public Alerta criarAlerta(String tipo, String severidade, String entidadeId, String descricao, String acaoSugerida) {
        Alerta alerta = new Alerta();
        alerta.setTipo(tipo);
        alerta.setSeveridade(severidade);
        alerta.setEntidadeId(entidadeId);
        alerta.setDescricao(descricao);
        alerta.setDataGerada(LocalDateTime.now());
        alerta.setStatus("ativo");
        alerta.setAcaoSugerida(acaoSugerida);

        return alertaRepository.save(alerta);
    }

    public void resolverAlerta(Long id) {
        alertaRepository.findById(id).ifPresent(alerta -> {
            alerta.setStatus("resolvido");
            alerta.setDataResolucao(LocalDateTime.now());
            alertaRepository.save(alerta);
        });
    }

    // TODO: Implementar detecção automática de atrasos e gargalos
    public void detectarAtrasos() {
        // Lógica de detecção de atrasos (>30min, >2h, >6h)
    }

    public void detectarGargalos() {
        // Lógica de detecção de gargalos de Gate, Yard e Equipamentos
    }
}