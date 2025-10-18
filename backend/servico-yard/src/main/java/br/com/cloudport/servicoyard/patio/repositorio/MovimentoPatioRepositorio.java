package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoPatioRepositorio extends JpaRepository<MovimentoPatio, Long> {
}
