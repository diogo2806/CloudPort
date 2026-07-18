package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.TruckHoppingSession;
import br.com.cloudport.servicogate.model.enums.TruckHoppingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckHoppingSessionRepository extends JpaRepository<TruckHoppingSession, Long> {

    Optional<TruckHoppingSession> findFirstByCpfMotoristaAndStatusOrderByCreatedAtDesc(
            String cpfMotorista, TruckHoppingStatus status);

    boolean existsByCpfMotoristaAndStatus(String cpfMotorista, TruckHoppingStatus status);

    List<TruckHoppingSession> findAllByOrderByCreatedAtDesc();
}
