package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.TelemetriaEquipamentoPatio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetriaEquipamentoPatioRepositorio extends JpaRepository<TelemetriaEquipamentoPatio, Long> {

    Optional<TelemetriaEquipamentoPatio> findByEquipamentoId(Long equipamentoId);

    Optional<TelemetriaEquipamentoPatio> findByEquipamentoIdentificador(String identificador);

    List<TelemetriaEquipamentoPatio> findAllByOrderByEquipamentoIdentificadorAsc();
}
