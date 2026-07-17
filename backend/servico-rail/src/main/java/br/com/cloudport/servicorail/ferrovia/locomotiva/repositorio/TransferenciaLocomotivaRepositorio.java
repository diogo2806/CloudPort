package br.com.cloudport.servicorail.ferrovia.locomotiva.repositorio;

import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.TransferenciaLocomotiva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaLocomotivaRepositorio extends JpaRepository<TransferenciaLocomotiva, Long> {

    boolean existsByVisitaTremIdAndIdentificadorLocomotivaIgnoreCase(Long visitaTremId,
                                                                     String identificadorLocomotiva);

    List<TransferenciaLocomotiva> findAllByOrderByCriadoEmDesc();
}
