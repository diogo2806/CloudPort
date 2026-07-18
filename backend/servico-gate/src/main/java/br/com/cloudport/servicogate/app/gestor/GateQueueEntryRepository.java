package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.GateQueueEntry;
import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import br.com.cloudport.servicogate.model.enums.GateQueueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateQueueEntryRepository extends JpaRepository<GateQueueEntry, Long> {

    Optional<GateQueueEntry> findFirstByGatePassIdAndSentidoAndStatusIn(
            Long gatePassId, GateQueueDirection sentido, Collection<GateQueueStatus> statuses);

    List<GateQueueEntry> findBySentidoAndStatusInOrderByPrioridadeDescPosicaoAtualAscEntrouEmAsc(
            GateQueueDirection sentido, Collection<GateQueueStatus> statuses);

    long countBySentidoAndStatusIn(GateQueueDirection sentido, Collection<GateQueueStatus> statuses);
}
