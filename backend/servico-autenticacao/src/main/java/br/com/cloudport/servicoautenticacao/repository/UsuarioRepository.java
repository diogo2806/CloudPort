package br.com.cloudport.servicoautenticacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cloudport.servicoautenticacao.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Aqui você pode adicionar métodos personalizados para consultas se necessário
}
