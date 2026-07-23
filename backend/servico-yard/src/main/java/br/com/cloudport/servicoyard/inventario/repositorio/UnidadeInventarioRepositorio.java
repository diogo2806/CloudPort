package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UnidadeInventarioRepositorio extends JpaRepository<UnidadeInventario, Long> {
    Optional<UnidadeInventario> findByIdentificacaoIgnoreCase(String identificacao);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UnidadeInventario> findComBloqueioByIdentificacaoIgnoreCase(String identificacao);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UnidadeInventario> findComBloqueioById(Long id);
    boolean existsByIdentificacaoIgnoreCase(String identificacao);
    long countByTipoEquipamentoId(Long tipoEquipamentoId);
    List<UnidadeInventario> findAllByOrderByIdentificacaoAsc();
}