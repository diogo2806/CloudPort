package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.model.Papel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelRepositorio extends JpaRepository<Papel, Long> {
    Optional<Papel> findByNome(String nome);
    List<Papel> findAllByOrderByIdAsc();
}
