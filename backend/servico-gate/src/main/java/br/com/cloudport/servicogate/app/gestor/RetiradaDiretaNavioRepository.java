package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.RetiradaDiretaNavio;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetiradaDiretaNavioRepository extends JpaRepository<RetiradaDiretaNavio, Long> {

    Optional<RetiradaDiretaNavio> findByCodigoAutorizacaoIgnoreCase(String codigoAutorizacao);

    Optional<RetiradaDiretaNavio> findByIdentificadorCargaIgnoreCase(String identificadorCarga);

    Page<RetiradaDiretaNavio> findAllByOrderBySaidaEmDesc(Pageable pageable);
}
