package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.EstadoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AvisoEstivagemPatioRepositorio extends JpaRepository<AvisoEstivagemPatio, Long> {

    Optional<AvisoEstivagemPatio> findByChaveEstavel(String chaveEstavel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AvisoEstivagemPatio> findComBloqueioByChaveEstavel(String chaveEstavel);

    List<AvisoEstivagemPatio> findAllByEstadoInOrderByAtualizadoEmDesc(
            Collection<EstadoAvisoEstivagemPatio> estados);

    List<AvisoEstivagemPatio> findByCodigoUnidadeIgnoreCaseAndEstadoIn(
            String codigoUnidade,
            Collection<EstadoAvisoEstivagemPatio> estados);

    List<AvisoEstivagemPatio> findByCodigoPosicaoInAndEstadoIn(
            Collection<String> codigosPosicao,
            Collection<EstadoAvisoEstivagemPatio> estados);

    boolean existsByCodigoUnidadeIgnoreCaseAndSeveridadeAndEstadoIn(
            String codigoUnidade,
            SeveridadeAvisoEstivagemPatio severidade,
            Collection<EstadoAvisoEstivagemPatio> estados);

    boolean existsByCodigoPosicaoInAndSeveridadeAndEstadoIn(
            Collection<String> codigosPosicao,
            SeveridadeAvisoEstivagemPatio severidade,
            Collection<EstadoAvisoEstivagemPatio> estados);
}
