package br.com.cloudport.serviconavio.escala.listatrabalho.repositorio;

import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.OrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.StatusOrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.TipoMovimentacaoOrdemNavio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemMovimentacaoNavioRepositorio extends JpaRepository<OrdemMovimentacaoNavio, Long> {

    List<OrdemMovimentacaoNavio> findByEscalaIdAndStatusMovimentacaoInOrderByCriadoEmAsc(
            Long idEscala, Collection<StatusOrdemMovimentacaoNavio> status);

    List<OrdemMovimentacaoNavio> findByEscalaId(Long idEscala);

    Optional<OrdemMovimentacaoNavio> findByEscalaIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(
            Long idEscala, String codigoConteiner, TipoMovimentacaoOrdemNavio tipoMovimentacao);

    Optional<OrdemMovimentacaoNavio> findByIdAndEscalaId(Long idOrdem, Long idEscala);

    boolean existsByEscalaIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(
            Long idEscala, String codigoConteiner, TipoMovimentacaoOrdemNavio tipoMovimentacao);
}
