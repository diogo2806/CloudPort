package br.com.cloudport.serviconavio.linha.repositorio;

import br.com.cloudport.serviconavio.linha.entidade.ServicoLinha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoLinhaRepositorio extends JpaRepository<ServicoLinha, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdentificadorNot(String codigo, Long identificador);

    List<ServicoLinha> findAllByOrderByCodigoAsc();
}
