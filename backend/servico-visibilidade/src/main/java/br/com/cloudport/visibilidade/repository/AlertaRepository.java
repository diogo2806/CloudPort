package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.Alerta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long>, JpaSpecificationExecutor<Alerta> {

    List<Alerta> findByStatusOrderByDataGeradaDesc(String status);

    List<Alerta> findByEntidadeIdAndStatus(String entidadeId, String status);

    long countByStatus(String status);

    long countByStatusAndSeveridadeIgnoreCase(String status, String severidade);

    long countByStatusAndDataReconhecimentoIsNull(String status);
}
