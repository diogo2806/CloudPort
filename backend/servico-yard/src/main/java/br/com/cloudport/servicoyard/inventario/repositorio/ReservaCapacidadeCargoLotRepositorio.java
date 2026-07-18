package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs.StatusReservaCapacidade;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaCapacidadeCargoLot;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaCapacidadeCargoLotRepositorio extends JpaRepository<ReservaCapacidadeCargoLot, UUID> {

    Optional<ReservaCapacidadeCargoLot> findByCommandId(UUID commandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaCapacidadeCargoLot> findComBloqueioById(UUID id);

    @Query("select coalesce(sum(r.quantidade), 0) from ReservaCapacidadeCargoLot r "
            + "where r.capacidade.id = :capacidadeId and r.status = :status")
    BigDecimal somarQuantidade(
            @Param("capacidadeId") UUID capacidadeId,
            @Param("status") StatusReservaCapacidade status);

    @Query("select coalesce(sum(r.volumeM3), 0) from ReservaCapacidadeCargoLot r "
            + "where r.capacidade.id = :capacidadeId and r.status = :status")
    BigDecimal somarVolume(
            @Param("capacidadeId") UUID capacidadeId,
            @Param("status") StatusReservaCapacidade status);

    @Query("select coalesce(sum(r.pesoKg), 0) from ReservaCapacidadeCargoLot r "
            + "where r.capacidade.id = :capacidadeId and r.status = :status")
    BigDecimal somarPeso(
            @Param("capacidadeId") UUID capacidadeId,
            @Param("status") StatusReservaCapacidade status);
}
