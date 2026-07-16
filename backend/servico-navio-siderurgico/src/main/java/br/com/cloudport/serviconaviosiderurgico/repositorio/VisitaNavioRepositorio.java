package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisitaNavioRepositorio extends JpaRepository<VisitaNavio, Long>, JpaSpecificationExecutor<VisitaNavio> {
    boolean existsByCodigoVisitaIgnoreCase(String codigoVisita);
    Optional<VisitaNavio> findByCodigoVisitaIgnoreCase(String codigoVisita);
    List<VisitaNavio> findAllByOrderByEtaDesc();
}
