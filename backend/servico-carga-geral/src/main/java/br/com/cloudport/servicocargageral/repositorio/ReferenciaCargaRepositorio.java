package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga;
import br.com.cloudport.servicocargageral.dominio.ReferenciaCarga;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenciaCargaRepositorio extends JpaRepository<ReferenciaCarga, UUID> {

    boolean existsByCategoriaAndCodigoIgnoreCase(CategoriaReferenciaCarga categoria, String codigo);

    List<ReferenciaCarga> findByCategoriaAndAtivoTrueOrderByCodigo(CategoriaReferenciaCarga categoria);

    List<ReferenciaCarga> findAllByOrderByCategoriaAscCodigoAsc();
}
