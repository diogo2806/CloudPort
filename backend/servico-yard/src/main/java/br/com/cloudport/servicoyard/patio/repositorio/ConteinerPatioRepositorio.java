package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConteinerPatioRepositorio extends JpaRepository<ConteinerPatio, Long> {

    Optional<ConteinerPatio> findByCodigo(String codigo);

    List<ConteinerPatio> findAllByOrderByPosicaoLinhaAscPosicaoColunaAsc();
}
