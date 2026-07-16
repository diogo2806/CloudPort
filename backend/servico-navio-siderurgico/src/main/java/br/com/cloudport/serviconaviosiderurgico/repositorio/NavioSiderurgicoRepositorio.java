package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusNavioSiderurgico;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavioSiderurgicoRepositorio extends JpaRepository<NavioSiderurgico, Long> {
    boolean existsByCodigoImoIgnoreCase(String codigoImo);
    boolean existsByNavioCadastroId(Long navioCadastroId);
    Optional<NavioSiderurgico> findByNavioCadastroId(Long navioCadastroId);
    List<NavioSiderurgico> findTop100ByAtualizadoEmBeforeAndStatusNotOrderByAtualizadoEmAsc(
            LocalDateTime atualizadoEm,
            StatusNavioSiderurgico status
    );
}
