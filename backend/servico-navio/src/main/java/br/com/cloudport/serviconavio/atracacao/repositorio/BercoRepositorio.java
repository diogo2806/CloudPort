package br.com.cloudport.serviconavio.atracacao.repositorio;

import br.com.cloudport.serviconavio.atracacao.entidade.Berco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BercoRepositorio extends JpaRepository<Berco, Long> {
    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdentificadorNot(String nome, Long identificador);
}
