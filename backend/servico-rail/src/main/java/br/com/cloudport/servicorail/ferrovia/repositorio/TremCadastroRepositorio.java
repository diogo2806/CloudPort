package br.com.cloudport.servicorail.ferrovia.repositorio;

import br.com.cloudport.servicorail.ferrovia.modelo.TremCadastro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TremCadastroRepositorio extends JpaRepository<TremCadastro, Long> {
    boolean existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCase(String operadora, String identificador);
    boolean existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCaseAndIdNot(String operadora, String identificador, Long id);

    @EntityGraph(attributePaths = "composicaoPadrao")
    Optional<TremCadastro> findWithComposicaoPadraoById(Long id);

    @EntityGraph(attributePaths = "composicaoPadrao")
    List<TremCadastro> findAllByOrderByOperadoraFerroviariaAscIdentificadorAsc();
}
