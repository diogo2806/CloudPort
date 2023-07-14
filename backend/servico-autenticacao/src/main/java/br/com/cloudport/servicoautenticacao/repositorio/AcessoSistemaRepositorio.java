package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.AcessoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcessoSistemaRepositorio extends JpaRepository<AcessoSistema, Long> {
    // Aqui você pode adicionar métodos personalizados de consulta, se necessário.
}
