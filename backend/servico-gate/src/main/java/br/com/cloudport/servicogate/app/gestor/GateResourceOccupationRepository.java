package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.GateResourceOccupation;
import br.com.cloudport.servicogate.model.enums.GateResourceType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateResourceOccupationRepository extends JpaRepository<GateResourceOccupation, Long> {

    Optional<GateResourceOccupation> findFirstByTipoRecursoAndChaveRecursoAndAtivoTrue(
            GateResourceType tipoRecurso, String chaveRecurso);

    List<GateResourceOccupation> findByGatePassIdAndAtivoTrue(Long gatePassId);

    List<GateResourceOccupation> findByTipoRecursoAndChaveRecursoInAndAtivoTrue(
            GateResourceType tipoRecurso, Collection<String> chaves);
}
