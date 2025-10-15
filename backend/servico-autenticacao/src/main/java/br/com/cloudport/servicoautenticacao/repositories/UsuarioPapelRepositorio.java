package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioPapelRepositorio extends JpaRepository<UsuarioPapel, Long> {
    boolean existsByPapelId(Long papelId);
}
