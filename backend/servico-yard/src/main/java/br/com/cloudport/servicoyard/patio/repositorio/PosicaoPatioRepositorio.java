package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosicaoPatioRepositorio extends JpaRepository<PosicaoPatio, Long> {

    Optional<PosicaoPatio> findByLinhaAndColunaAndCamadaOperacional(Integer linha, Integer coluna, String camadaOperacional);
}
