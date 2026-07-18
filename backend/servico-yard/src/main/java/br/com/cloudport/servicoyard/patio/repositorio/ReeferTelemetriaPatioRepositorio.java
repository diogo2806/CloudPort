package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.ReeferTelemetriaPatio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReeferTelemetriaPatioRepositorio extends JpaRepository<ReeferTelemetriaPatio, Long> {

    Optional<ReeferTelemetriaPatio> findByConteinerId(Long conteinerId);

    @EntityGraph(attributePaths = {"conteiner", "conteiner.posicao"})
    List<ReeferTelemetriaPatio> findAllByOrderByRegistradoEmDesc();
}