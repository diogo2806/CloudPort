package br.com.cloudport.servicoyard.patio.custodia.repositorio;

import br.com.cloudport.servicoyard.patio.custodia.modelo.CustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.modelo.StatusCustodiaExchangeArea;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustodiaExchangeAreaRepositorio extends JpaRepository<CustodiaExchangeArea, Long> {

    List<CustodiaExchangeArea> findAllByOrderByAtualizadoEmDesc();

    List<CustodiaExchangeArea> findAllByStatusOrderByAtualizadoEmDesc(StatusCustodiaExchangeArea status);

    Optional<CustodiaExchangeArea> findByChaveIdempotenciaEntrega(String chaveIdempotencia);

    Optional<CustodiaExchangeArea> findByChaveIdempotenciaRecebimento(String chaveIdempotencia);

    Optional<CustodiaExchangeArea> findFirstByCodigoUnidadeIgnoreCaseAndStatusInOrderByCriadoEmDesc(
            String codigoUnidade,
            Collection<StatusCustodiaExchangeArea> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustodiaExchangeArea c where c.id = :id")
    Optional<CustodiaExchangeArea> findByIdForUpdate(@Param("id") Long id);
}
