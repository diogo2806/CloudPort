package br.com.cloudport.servicoyard.dispatch.repositorio;

import br.com.cloudport.servicoyard.dispatch.modelo.InstrucaoMovimentacao;
import br.com.cloudport.servicoyard.dispatch.modelo.StatusInstrucaoMovimentacao;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrucaoMovimentacaoRepositorio extends JpaRepository<InstrucaoMovimentacao, Long> {

    List<InstrucaoMovimentacao> findAllByOrderBySequenciaAscCriadoEmAsc();

    /**
     * Job list de um equipamento (CHE): instruções nos estados informados,
     * com fetch prioritário primeiro e depois pela sequência de dispatch.
     */
    List<InstrucaoMovimentacao> findByEquipamentoIdAndStatusInOrderByPrioridadeFetchDescSequenciaAscCriadoEmAsc(
            Long equipamentoId, Collection<StatusInstrucaoMovimentacao> status);
}
