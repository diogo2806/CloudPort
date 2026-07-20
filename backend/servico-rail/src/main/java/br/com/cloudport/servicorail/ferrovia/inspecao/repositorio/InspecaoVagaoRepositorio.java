package br.com.cloudport.servicorail.ferrovia.inspecao.repositorio;

import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspecaoVagaoRepositorio extends JpaRepository<InspecaoVagao, Long> {

    List<InspecaoVagao> findByVisitaTremIdOrderByInspecionadoEmDesc(Long idVisita);

    Optional<InspecaoVagao> findFirstByVisitaTremIdAndIdentificadorVagaoIgnoreCaseOrderByInspecionadoEmDesc(
            Long idVisita,
            String identificadorVagao);

    Optional<InspecaoVagao> findByIdAndVisitaTremId(Long id, Long idVisita);
}
