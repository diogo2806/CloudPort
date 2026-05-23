package br.com.cloudport.serviconavio.atracacao.repositorio;

import br.com.cloudport.serviconavio.atracacao.entidade.OperacaoNavioConteiner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoNavioConteinerRepositorio extends JpaRepository<OperacaoNavioConteiner, Long> {
}
