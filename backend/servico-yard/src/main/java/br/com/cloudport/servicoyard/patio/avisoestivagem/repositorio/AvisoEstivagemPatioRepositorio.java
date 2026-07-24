package br.com.cloudport.servicoyard.patio.avisoestivagem.repositorio;

import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvisoEstivagemPatioRepositorio extends JpaRepository<AvisoEstivagemPatio, Long> {

    Optional<AvisoEstivagemPatio> findByChaveEstavel(String chaveEstavel);

    List<AvisoEstivagemPatio> findAllByOrderByAtualizadoEmDesc();

    List<AvisoEstivagemPatio> findBySeveridadeAndStatusIn(
            SeveridadeAvisoEstivagemPatio severidade,
            Collection<StatusAvisoEstivagemPatio> status);
}
