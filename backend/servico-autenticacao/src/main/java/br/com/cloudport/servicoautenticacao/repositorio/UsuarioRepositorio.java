package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    // Aqui você pode adicionar métodos personalizados para consultas se necessário
}
