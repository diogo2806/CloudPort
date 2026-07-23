package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.Empresa;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByDocumentoNormalizado(String documentoNormalizado);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, UUID id);
    boolean existsByDocumentoNormalizadoAndIdNot(String documentoNormalizado, UUID id);
    List<Empresa> findAllByOrderByRazaoSocialAsc();
}