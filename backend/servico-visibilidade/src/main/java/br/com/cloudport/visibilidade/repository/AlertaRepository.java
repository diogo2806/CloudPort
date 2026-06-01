package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.Alerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByStatusOrderByDataGeradaDesc(String status);

    Page<Alerta> findBySeveridadeInAndTipoInAndStatus(List<String> severidades, List<String> tipos, String status, Pageable pageable);

    List<Alerta> findByEntidadeIdAndStatus(String entidadeId, String status);
}