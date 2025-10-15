package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.GateOcorrencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateOcorrenciaRepository extends JpaRepository<GateOcorrencia, Long> {

    List<GateOcorrencia> findTop100ByOrderByRegistradoEmDesc();

    List<GateOcorrencia> findByVeiculoIdOrderByRegistradoEmDesc(Long veiculoId);
}
