package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemCargaSiderurgica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCargaSiderurgicaRepositorio extends JpaRepository<ItemCargaSiderurgica, Long> {
    List<ItemCargaSiderurgica> findByOperacaoIdOrderBySequenciaOperacionalAscIdAsc(Long operacaoId);
    boolean existsByOperacaoIdAndCodigoLoteIgnoreCase(Long operacaoId, String codigoLote);
}
