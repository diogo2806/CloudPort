package br.com.cloudport.servicoyard.container.repositorio;

import br.com.cloudport.servicoyard.container.entidade.Conteiner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConteinerRepositorio extends JpaRepository<Conteiner, Long> {
    Optional<Conteiner> findByIdentificacaoIgnoreCase(String identificacao);
}
