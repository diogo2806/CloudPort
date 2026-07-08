package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaPosicaoPatioNavioRepositorio extends JpaRepository<ReservaPosicaoPatioNavio, Long> {
    List<ReservaPosicaoPatioNavio> findByVisitaNavioIdOrderByCriadoEmAsc(Long visitaNavioId);
    List<ReservaPosicaoPatioNavio> findByVisitaNavioIdAndStatusOrderByCriadoEmAsc(Long visitaNavioId, StatusReservaPatioNavio status);
    Optional<ReservaPosicaoPatioNavio> findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(Long itemOperacaoNavioId, Collection<StatusReservaPatioNavio> status);
    boolean existsByPosicaoPatioIdIgnoreCaseAndStatusIn(String posicaoPatioId, Collection<StatusReservaPatioNavio> status);
    long countByVisitaNavioIdAndStatus(Long visitaNavioId, StatusReservaPatioNavio status);
}
