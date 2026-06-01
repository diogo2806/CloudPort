package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoMovimentoRepository extends JpaRepository<HistoricoMovimento, Long> {

    List<HistoricoMovimento> findByContainerIdOrderByTimestampDesc(String containerId);

    Page<HistoricoMovimento> findByContainerId(String containerId, Pageable pageable);
}