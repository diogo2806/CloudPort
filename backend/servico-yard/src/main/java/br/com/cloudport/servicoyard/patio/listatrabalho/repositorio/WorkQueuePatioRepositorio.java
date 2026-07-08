package br.com.cloudport.servicoyard.patio.listatrabalho.repositorio;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkQueuePatioRepositorio extends JpaRepository<WorkQueuePatio, Long> {

    List<WorkQueuePatio> findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(Long visitaNavioId);

    Optional<WorkQueuePatio> findByIdentificadorIgnoreCase(String identificador);
}
