package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.ReservaConteinerCargaGeral;
import br.com.cloudport.servicoyard.inventario.modelo.ReservaConteinerCargaGeral.StatusReserva;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ReservaConteinerCargaGeralRepositorio
        extends JpaRepository<ReservaConteinerCargaGeral, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaConteinerCargaGeral> findByOperacaoId(UUID operacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaConteinerCargaGeral> findByUnidadeIdAndStatus(Long unidadeId, StatusReserva status);

    List<ReservaConteinerCargaGeral> findAllByStatus(StatusReserva status);
}
