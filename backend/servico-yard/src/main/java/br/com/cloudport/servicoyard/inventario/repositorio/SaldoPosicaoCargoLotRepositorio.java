package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.SaldoPosicaoCargoLot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SaldoPosicaoCargoLotRepositorio extends JpaRepository<SaldoPosicaoCargoLot, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SaldoPosicaoCargoLot> findComBloqueioByCapacidade_IdAndLoteId(UUID capacidadeId, UUID loteId);

    List<SaldoPosicaoCargoLot> findByCapacidade_IdOrderByAtualizadoEmDesc(UUID capacidadeId);

    List<SaldoPosicaoCargoLot> findByCapacidade_Id(UUID capacidadeId);

    default BigDecimal somarQuantidade(UUID capacidadeId) {
        return findByCapacidade_Id(capacidadeId).stream()
                .map(SaldoPosicaoCargoLot::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal somarVolume(UUID capacidadeId) {
        return findByCapacidade_Id(capacidadeId).stream()
                .map(SaldoPosicaoCargoLot::getVolumeM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal somarPeso(UUID capacidadeId) {
        return findByCapacidade_Id(capacidadeId).stream()
                .map(SaldoPosicaoCargoLot::getPesoKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
