package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.StatusNavioDTO;
import br.com.cloudport.visibilidade.entity.StatusNavio;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatusNavioService {

    @Autowired
    private StatusNavioRepository statusNavioRepository;

    public StatusNavioDTO getStatusNavio(String navioId) {
        StatusNavio navio = statusNavioRepository.findByNavioId(navioId)
                .orElseThrow(() -> new RuntimeException("Navio não encontrado: " + navioId));

        // TODO: Mapear para DTO completo com ETA, berço, operações, equipamentos, alertas e timeline
        StatusNavioDTO dto = new StatusNavioDTO();
        dto.setNavioId(navio.getNavioId());
        dto.setNomeNavio(navio.getNomeNavio());
        dto.setStatusOperacional(navio.getStatusOperacional());

        return dto;
    }

    public List<StatusNavioDTO> listarNaviosEmOperacao() {
        return statusNavioRepository.findByStatusOperacional("operando").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private StatusNavioDTO mapToDTO(StatusNavio entity) {
        StatusNavioDTO dto = new StatusNavioDTO();
        dto.setNavioId(entity.getNavioId());
        dto.setNomeNavio(entity.getNomeNavio());
        dto.setStatusOperacional(entity.getStatusOperacional());
        return dto;
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
}