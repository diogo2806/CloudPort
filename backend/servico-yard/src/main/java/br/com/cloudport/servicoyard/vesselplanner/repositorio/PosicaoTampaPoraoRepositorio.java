package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.PosicaoTampaPorao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosicaoTampaPoraoRepositorio extends JpaRepository<PosicaoTampaPorao, Long> {

    List<PosicaoTampaPorao> findByTampaIdOrderByInicioEmAsc(Long tampaId);

    Optional<PosicaoTampaPorao> findFirstByTampaIdAndAtivaTrueOrderByInicioEmDesc(Long tampaId);
}
