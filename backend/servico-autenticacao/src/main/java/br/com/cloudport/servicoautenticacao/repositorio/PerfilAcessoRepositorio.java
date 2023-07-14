package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.PerfilAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilAcessoRepositorio extends JpaRepository<PerfilAcesso, Long> {
    // Aqui você pode adicionar métodos personalizados de consulta, se necessário.
}
