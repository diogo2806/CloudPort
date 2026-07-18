package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao;
import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao.StatusPlano;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoStuffUnstuffVersaoRepositorio extends JpaRepository<PlanoStuffUnstuffVersao, UUID> {

    @EntityGraph(attributePaths = "itens")
    List<PlanoStuffUnstuffVersao> findByOperacao_IdOrderByNumeroVersaoDesc(UUID operacaoId);

    @EntityGraph(attributePaths = "itens")
    Optional<PlanoStuffUnstuffVersao> findFirstByOperacao_IdOrderByNumeroVersaoDesc(UUID operacaoId);

    @EntityGraph(attributePaths = "itens")
    Optional<PlanoStuffUnstuffVersao> findByOperacao_IdAndNumeroVersao(UUID operacaoId, int numeroVersao);

    boolean existsByOperacao_IdAndStatus(UUID operacaoId, StatusPlano status);
}
