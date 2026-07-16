package br.com.cloudport.servicoyard.patio.listatrabalho.repositorio;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.AplicacaoPlanoOtimizadoPatio;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AplicacaoPlanoOtimizadoPatioRepositorio
        extends JpaRepository<AplicacaoPlanoOtimizadoPatio, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AplicacaoPlanoOtimizadoPatio> findByPlanoIdAndVisitaNavioId(
            String planoId,
            Long visitaNavioId
    );
}
