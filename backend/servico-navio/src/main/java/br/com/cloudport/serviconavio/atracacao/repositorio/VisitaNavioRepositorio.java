package br.com.cloudport.serviconavio.atracacao.repositorio;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusVisitaNavio;
import br.com.cloudport.serviconavio.atracacao.entidade.VisitaNavio;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitaNavioRepositorio extends JpaRepository<VisitaNavio, Long> {

    List<VisitaNavio> findAllByOrderByAtracacaoPrevistaAsc();

    List<VisitaNavio> findByBercoIsNotNullOrderByAtracacaoPrevistaAsc();

    /**
     * Localiza visitas que ocupam o mesmo berço com janela de atracação sobreposta
     * à janela informada (sobreposição quando existente.atracacao < nova.desatracacao
     * e existente.desatracacao > nova.atracacao), ignorando a própria visita e estados finais.
     */
    @Query("select v from VisitaNavio v "
            + "where v.berco.identificador = :bercoId "
            + "and v.identificador <> :visitaId "
            + "and v.status not in :statusIgnorados "
            + "and v.atracacaoPrevista < :fim "
            + "and v.desatracacaoPrevista > :inicio")
    List<VisitaNavio> buscarConflitosDeAtracacao(@Param("bercoId") Long bercoId,
                                                 @Param("visitaId") Long visitaId,
                                                 @Param("statusIgnorados") Collection<StatusVisitaNavio> statusIgnorados,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fim") LocalDateTime fim);
}
