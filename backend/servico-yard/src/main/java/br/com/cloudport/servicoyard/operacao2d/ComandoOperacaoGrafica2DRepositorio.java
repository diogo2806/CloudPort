package br.com.cloudport.servicoyard.operacao2d;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComandoOperacaoGrafica2DRepositorio extends JpaRepository<ComandoOperacaoGrafica2D, Long> {

    Optional<ComandoOperacaoGrafica2D> findByCommandId(String commandId);
}
