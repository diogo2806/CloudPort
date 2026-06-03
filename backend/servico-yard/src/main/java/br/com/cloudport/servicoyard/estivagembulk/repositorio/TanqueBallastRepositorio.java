package br.com.cloudport.servicoyard.estivagembulk.repositorio;

import br.com.cloudport.servicoyard.estivagembulk.modelo.TanqueBallast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TanqueBallastRepositorio extends JpaRepository<TanqueBallast, Long> {

    List<TanqueBallast> findByNavioIdOrderByPosLongCentroMAsc(Long navioId);
}
