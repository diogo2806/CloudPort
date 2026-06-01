package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.Optional;

@Service
public class CapacidadeYardService {

    @Autowired
    private CapacidadeYardRepository capacidadeYardRepository;

    public CapacidadeYard getCapacidadePorZona(String zona) {
        return capacidadeYardRepository.findByZona(zona)
                .orElseThrow(() -> new RuntimeException("Zona não encontrada: " + zona));
    }

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
        }
    }

    // TODO: Implementar lógica de detecção de gargalos (>85%, >95%)
    public void verificarGargalos() {
        // Verificar zonas com ocupação crítica
    }
}