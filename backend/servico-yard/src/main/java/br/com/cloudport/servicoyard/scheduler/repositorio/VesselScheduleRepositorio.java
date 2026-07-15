package br.com.cloudport.servicoyard.scheduler.repositorio;

import br.com.cloudport.servicoyard.scheduler.modelo.VesselSchedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VesselScheduleRepositorio extends JpaRepository<VesselSchedule, Long> {

    List<VesselSchedule> findAllByOrderByTempoPrevistoAsc();

    List<VesselSchedule> findByTempoTerminoAfterAndTempoPrevistoBeforeOrderByTempoPrevistoAsc(
            LocalDateTime inicio,
            LocalDateTime fim);
}
