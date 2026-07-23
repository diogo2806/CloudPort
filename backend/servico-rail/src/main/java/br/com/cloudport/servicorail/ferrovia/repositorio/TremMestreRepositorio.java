package br.com.cloudport.servicorail.ferrovia.repositorio;

import br.com.cloudport.servicorail.ferrovia.modelo.TremMestre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TremMestreRepositorio extends JpaRepository<TremMestre, Long> {

    boolean existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCase(String operadora, String identificador);

    boolean existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCaseAndIdNot(
            String operadora, String identificador, Long id);

    @EntityGraph(attributePaths = "composicaoPadrao")
    Optional<TremMestre> findWithComposicaoPadraoById(Long id);

    @EntityGraph(attributePaths = "composicaoPadrao")
    List<TremMestre> findAllByOrderByOperadoraFerroviariaAscIdentificadorAsc();

    @EntityGraph(attributePaths = "composicaoPadrao")
    List<TremMestre> findByAtivoTrueOrderByOperadoraFerroviariaAscIdentificadorAsc();
}
