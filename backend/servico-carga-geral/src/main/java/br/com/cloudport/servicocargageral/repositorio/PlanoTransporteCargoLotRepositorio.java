package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.ModalTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.PlanoTransporteCargoLot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PlanoTransporteCargoLotRepositorio extends JpaRepository<PlanoTransporteCargoLot, UUID> {

    Optional<PlanoTransporteCargoLot> findByCommandIdPlanejamento(UUID commandIdPlanejamento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlanoTransporteCargoLot> findComBloqueioById(UUID id);

    List<PlanoTransporteCargoLot> findByModalAndVisitaIdOrderBySequenciaAsc(
            ModalTransporteCargo modal,
            String visitaId);
}
