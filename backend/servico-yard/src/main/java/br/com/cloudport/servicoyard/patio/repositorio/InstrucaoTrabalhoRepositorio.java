package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrucaoTrabalhoRepositorio extends JpaRepository<InstrucaoTrabalho, Long> {

    boolean existsByDestinoIgnoreCaseAndStatusIn(String destino, Collection<StatusInstrucao> statuses);

    List<InstrucaoTrabalho> findByStatusOrderByCreatedAtDesc(StatusInstrucao status);

    List<InstrucaoTrabalho> findByCodigoConteinerIgnoreCaseOrderByCreatedAtDesc(String codigoConteiner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InstrucaoTrabalho i where i.id = :id")
    Optional<InstrucaoTrabalho> findByIdForUpdate(@Param("id") Long id);
}
