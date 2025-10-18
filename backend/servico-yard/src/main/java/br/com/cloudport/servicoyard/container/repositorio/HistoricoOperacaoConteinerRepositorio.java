package br.com.cloudport.servicoyard.container.repositorio;

import br.com.cloudport.servicoyard.container.entidade.HistoricoOperacaoConteiner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoOperacaoConteinerRepositorio extends JpaRepository<HistoricoOperacaoConteiner, Long> {
    List<HistoricoOperacaoConteiner> findByConteinerIdOrderByDataRegistroDesc(Long conteinerId);
}
