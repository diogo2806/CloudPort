package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrucaoTrabalhoRepositorio extends JpaRepository<InstrucaoTrabalho, Long> {

    boolean existsByDestinoIgnoreCaseAndStatusIn(String destino, Collection<StatusInstrucao> statuses);

    List<InstrucaoTrabalho> findByStatusOrderByCreatedAtDesc(StatusInstrucao status);

    List<InstrucaoTrabalho> findByCodigoConteinerIgnoreCaseOrderByCreatedAtDesc(String codigoConteiner);
}
