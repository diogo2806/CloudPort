package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.AplicacaoPlanoOtimizadoNavioPatio;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AplicacaoPlanoOtimizadoNavioPatioRepositorio
        extends JpaRepository<AplicacaoPlanoOtimizadoNavioPatio, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AplicacaoPlanoOtimizadoNavioPatio> findByPlanoIdAndVisitaNavioId(
            String planoId,
            Long visitaNavioId
    );
}
