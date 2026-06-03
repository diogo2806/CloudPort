package br.com.cloudport.servicoyard.estivagembulk.repositorio;

import br.com.cloudport.servicoyard.estivagembulk.modelo.PortoViagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortoViagemRepositorio extends JpaRepository<PortoViagem, Long> {

    List<PortoViagem> findByPlanoIdOrderBySequenciaAsc(Long planoId);
}
