package br.com.cloudport.servicoyard.estivagembulk.repositorio;

import br.com.cloudport.servicoyard.estivagembulk.modelo.ClasseNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavioGranelRepositorio extends JpaRepository<NavioGranel, Long> {

    List<NavioGranel> findByIsTemplateTrue();

    List<NavioGranel> findByClasseAndIsTemplateTrue(ClasseNavio classe);

    Optional<NavioGranel> findByImo(String imo);

    Optional<NavioGranel> findTopByNavioCadastroIdOrderByVersaoPerfilDesc(Long navioCadastroId);

    Optional<NavioGranel> findByNavioCadastroIdAndVersaoPerfil(Long navioCadastroId, Long versaoPerfil);

    List<NavioGranel> findByIsTemplateFalseOrderByNomeAsc();
}
