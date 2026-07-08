package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOperacaoNavioRepositorio extends JpaRepository<ItemOperacaoNavio, Long> {
    List<ItemOperacaoNavio> findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(Long visitaNavioId);
    List<ItemOperacaoNavio> findByVisitaNavioId(Long visitaNavioId);
    boolean existsByVisitaNavioIdAndTipoMovimentoAndCodigoLoteIgnoreCase(Long visitaNavioId, TipoMovimentoNavio tipoMovimento, String codigoLote);
    long countByVisitaNavioId(Long visitaNavioId);
}
