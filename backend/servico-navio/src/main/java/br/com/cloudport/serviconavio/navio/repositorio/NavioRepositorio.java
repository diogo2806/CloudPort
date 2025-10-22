package br.com.cloudport.serviconavio.navio.repositorio;

import br.com.cloudport.serviconavio.navio.entidade.Navio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavioRepositorio extends JpaRepository<Navio, Long> {
    boolean existsByCodigoImoIgnoreCase(String codigoImo);
    boolean existsByCodigoImoIgnoreCaseAndIdentificadorNot(String codigoImo, Long identificador);
}
