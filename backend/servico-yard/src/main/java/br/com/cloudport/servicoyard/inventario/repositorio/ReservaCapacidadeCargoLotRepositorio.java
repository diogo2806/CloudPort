package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.StatusReservaCapacidade;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaCapacidadeCargoLot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ReservaCapacidadeCargoLotRepositorio extends JpaRepository<ReservaCapacidadeCargoLot, UUID> {

    Optional<ReservaCapacidadeCargoLot> findByCommandId(UUID commandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaCapacidadeCargoLot> findComBloqueioById(UUID id);

    List<ReservaCapacidadeCargoLot> findByCapacidade_IdAndStatus(
            UUID capacidadeId,
            StatusReservaCapacidade status);

    default BigDecimal somarQuantidade(UUID capacidadeId, StatusReservaCapacidade status) {
        return findByCapacidade_IdAndStatus(capacidadeId, status).stream()
                .map(ReservaCapacidadeCargoLot::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal somarVolume(UUID capacidadeId, StatusReservaCapacidade status) {
        return findByCapacidade_IdAndStatus(capacidadeId, status).stream()
                .map(ReservaCapacidadeCargoLot::getVolumeM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal somarPeso(UUID capacidadeId, StatusReservaCapacidade status) {
        return findByCapacidade_IdAndStatus(capacidadeId, status).stream()
                .map(ReservaCapacidadeCargoLot::getPesoKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
