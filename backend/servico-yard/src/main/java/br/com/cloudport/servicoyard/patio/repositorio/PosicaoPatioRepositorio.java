package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PosicaoPatioRepositorio extends JpaRepository<PosicaoPatio, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PosicaoPatio> findByLinhaAndColunaAndCamadaOperacional(
            Integer linha,
            Integer coluna,
            String camadaOperacional);

    List<PosicaoPatio> findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc();
}
