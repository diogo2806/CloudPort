package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.ReconciliacaoBaplieExecucao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliacaoBaplieExecucaoRepositorio
        extends JpaRepository<ReconciliacaoBaplieExecucao, Long> {

    Optional<ReconciliacaoBaplieExecucao> findTopByPlanoIdOrderByExecutadaEmDesc(Long planoId);

    List<ReconciliacaoBaplieExecucao> findByPlanoIdOrderByExecutadaEmDesc(Long planoId);
}
