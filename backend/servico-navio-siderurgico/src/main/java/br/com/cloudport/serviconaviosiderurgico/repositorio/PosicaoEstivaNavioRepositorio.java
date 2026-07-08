package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.PosicaoEstivaNavio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosicaoEstivaNavioRepositorio extends JpaRepository<PosicaoEstivaNavio, Long> {
    List<PosicaoEstivaNavio> findByPlanoEstivaIdOrderBySequenciaAscIdAsc(Long planoEstivaId);
    void deleteByPlanoEstivaId(Long planoEstivaId);
}
