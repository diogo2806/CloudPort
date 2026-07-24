package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga.TipoRecursoCarga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoEmpresaCargaRepositorio extends JpaRepository<VinculoEmpresaCarga, UUID> {

    List<VinculoEmpresaCarga> findByTipoRecursoAndRecursoIdOrderByPapelAsc(
            TipoRecursoCarga tipoRecurso,
            UUID recursoId);

    Optional<VinculoEmpresaCarga> findByTipoRecursoAndRecursoIdAndPapel(
            TipoRecursoCarga tipoRecurso,
            UUID recursoId,
            PapelEmpresa papel);
}
