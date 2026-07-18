package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.CapacidadePosicaoCargoLot;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CapacidadePosicaoCargoLotRepositorio extends JpaRepository<CapacidadePosicaoCargoLot, UUID> {

    Optional<CapacidadePosicaoCargoLot> findByPosicaoIgnoreCase(String posicao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CapacidadePosicaoCargoLot> findComBloqueioByPosicaoIgnoreCase(String posicao);
}
