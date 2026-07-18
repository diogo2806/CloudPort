package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.ConfirmacaoReservaGateCarga;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmacaoReservaGateCargaRepositorio extends JpaRepository<ConfirmacaoReservaGateCarga, UUID> {
    Optional<ConfirmacaoReservaGateCarga> findByConfirmacaoIdIgnoreCase(String confirmacaoId);
}
