package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapacidadeYardService {

    @Autowired
    private CapacidadeYardRepository capacidadeYardRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    public CapacidadeYard getCapacidadePorZona(String zona) {
        return capacidadeYardRepository.findByZona(zona)
                .orElseThrow(() -> new IllegalArgumentException("Zona nao encontrada: " + zona));
    }

    @Transactional
    public void atualizarOcupacao(String zona, int novaOcupacao) {
        Optional<CapacidadeYard> optional = capacidadeYardRepository.findByZona(zona);
        if (optional.isPresent()) {
            CapacidadeYard yard = optional.get();
            yard.setOcupacaoAtual(novaOcupacao);
            if (yard.getCapacidadeTotal() != null && yard.getCapacidadeTotal() > 0) {
                yard.setPercentualOcupacao((novaOcupacao * 100.0) / yard.getCapacidadeTotal());
            }
            yard.setDataAtualizacao(LocalDateTime.now());
            capacidadeYardRepository.save(yard);
            verificarGargalos();
        }
    }

    @Transactional
    public void verificarGargalos() {
        List<CapacidadeYard> zonas = capacidadeYardRepository.findAll();
        for (CapacidadeYard zona : zonas) {
            double percentual = calcularPercentual(zona);
            String entidadeId = "YARD-" + zona.getZona();
            List<Alerta> alertasAtivos = alertaRepository.findByEntidadeIdAndStatus(entidadeId, "ativo");
            boolean existeAlerta = alertasAtivos.stream()
                    .anyMatch(alerta -> "GARGALO_YARD".equalsIgnoreCase(alerta.getTipo()));

            if (percentual >= 85d) {
                if (existeAlerta) {
                    atualizarAlertaExistente(alertasAtivos, zona, percentual);
                } else {
                    criarAlerta(zona, percentual);
                }
                continue;
            }

            alertasAtivos.stream()
                    .filter(alerta -> "GARGALO_YARD".equalsIgnoreCase(alerta.getTipo()))
                    .forEach(this::resolverAlerta);
        }
    }

    private double calcularPercentual(CapacidadeYard yard) {
        if (yard.getPercentualOcupacao() != null) {
            return yard.getPercentualOcupacao();
        }
        if (yard.getCapacidadeTotal() == null || yard.getCapacidadeTotal() <= 0 || yard.getOcupacaoAtual() == null) {
            return 0d;
        }
        return (yard.getOcupacaoAtual() * 100.0) / yard.getCapacidadeTotal();
    }

    private void atualizarAlertaExistente(List<Alerta> alertasAtivos, CapacidadeYard zona, double percentual) {
        alertasAtivos.stream()
                .filter(alerta -> "GARGALO_YARD".equalsIgnoreCase(alerta.getTipo()))
                .forEach(alerta -> {
                    alerta.setSeveridade(percentual >= 95d ? "critica" : "alta");
                    alerta.setDescricao("Zona " + zona.getZona() + " com ocupacao em " + round(percentual) + "%");
                    alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saidas.");
                    alertaRepository.save(alerta);
                });
    }

    private void criarAlerta(CapacidadeYard zona, double percentual) {
        Alerta alerta = new Alerta();
        alerta.setTipo("GARGALO_YARD");
        alerta.setSeveridade(percentual >= 95d ? "critica" : "alta");
        alerta.setEntidadeId("YARD-" + zona.getZona());
        alerta.setDescricao("Zona " + zona.getZona() + " com ocupacao em " + round(percentual) + "%");
        alerta.setDataGerada(LocalDateTime.now());
        alerta.setStatus("ativo");
        alerta.setAcaoSugerida("Bloquear novas entradas e priorizar saidas.");
        alertaRepository.save(alerta);
    }

    private void resolverAlerta(Alerta alerta) {
        alerta.setStatus("resolvido");
        alerta.setDataResolucao(LocalDateTime.now());
        alertaRepository.save(alerta);
    }

    private double round(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
