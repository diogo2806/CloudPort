package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.GateCall;
import br.com.cloudport.servicogate.model.enums.GateCallStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateCallRepository extends JpaRepository<GateCall, Long> {

    Optional<GateCall> findFirstByGatePassIdAndStatusIn(Long gatePassId, Collection<GateCallStatus> statuses);

    List<GateCall> findAllByOrderByChamadoEmDesc();
}
