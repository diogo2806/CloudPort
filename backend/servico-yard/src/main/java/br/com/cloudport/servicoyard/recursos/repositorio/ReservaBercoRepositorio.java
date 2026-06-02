package br.com.cloudport.servicoyard.recursos.repositorio;

import br.com.cloudport.servicoyard.recursos.entidade.ReservaBerco;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaBercoRepositorio extends JpaRepository<ReservaBerco, Long> {

    List<ReservaBerco> findByBercoCodigoOrderByChegadaPrevistaAsc(String bercoCodigo);

    List<ReservaBerco> findByChegadaPrevistaLessThanAndSaidaPrevistaGreaterThan(LocalDateTime fim, LocalDateTime inicio);
}
