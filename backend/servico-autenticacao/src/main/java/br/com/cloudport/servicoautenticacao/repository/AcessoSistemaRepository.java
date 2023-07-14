package br.com.cloudport.servicoautenticacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cloudport.servicoautenticacao.model.AcessoSistema;

@Repository
public interface AcessoSistemaRepository extends JpaRepository<AcessoSistema, Long> {
    // Aqui você pode adicionar métodos personalizados de consulta, se necessário.
}
