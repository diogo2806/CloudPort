package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.model.GateEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateEventRepository extends JpaRepository<GateEvent, Long> {

    List<GateEvent> findByGatePassIdOrderByRegistradoEmAsc(Long gatePassId);

    List<GateEvent> findTop100ByOrderByRegistradoEmDesc();
}
