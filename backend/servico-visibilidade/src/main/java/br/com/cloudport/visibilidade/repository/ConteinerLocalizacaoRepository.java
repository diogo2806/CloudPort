package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConteinerLocalizacaoRepository extends JpaRepository<ConteinerLocalizacao, Long> {

    Optional<ConteinerLocalizacao> findByContainerId(String containerId);

    boolean existsByContainerId(String containerId);
}