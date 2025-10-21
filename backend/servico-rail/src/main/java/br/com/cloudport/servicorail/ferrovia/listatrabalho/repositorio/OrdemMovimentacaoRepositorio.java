package br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemMovimentacaoRepositorio extends JpaRepository<OrdemMovimentacao, Long> {

    List<OrdemMovimentacao> findByVisitaTremIdAndStatusMovimentacaoInOrderByCriadoEmAsc(Long idVisita,
                                                                                        Collection<StatusOrdemMovimentacao> status);

    List<OrdemMovimentacao> findByVisitaTremId(Long idVisita);

    Optional<OrdemMovimentacao> findByVisitaTremIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(Long idVisita,
                                                                                                  String codigoConteiner,
                                                                                                  TipoMovimentacaoOrdem tipoMovimentacao);

    Optional<OrdemMovimentacao> findByIdAndVisitaTremId(Long idOrdem, Long idVisita);

    boolean existsByVisitaTremIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(Long idVisita,
                                                                                String codigoConteiner,
                                                                                TipoMovimentacaoOrdem tipoMovimentacao);
}
