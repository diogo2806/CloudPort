package br.com.cloudport.servicoyard.patio.listatrabalho.repositorio;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoWorkInstructionRepositorio extends JpaRepository<HistoricoOperacaoPatio, Long> {

    List<HistoricoOperacaoPatio> findTop100ByOrdemTrabalhoPatioIdOrderByCriadoEmDesc(Long ordemTrabalhoPatioId);
}
