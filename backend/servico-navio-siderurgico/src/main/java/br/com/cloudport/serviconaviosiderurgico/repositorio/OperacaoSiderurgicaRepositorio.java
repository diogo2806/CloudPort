package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.OperacaoSiderurgica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoSiderurgicaRepositorio extends JpaRepository<OperacaoSiderurgica, Long> {
    List<OperacaoSiderurgica> findByNavioIdOrderByEtaDesc(Long navioId);
}
