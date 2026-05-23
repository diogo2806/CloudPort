package br.com.cloudport.serviconavio.estiva.repositorio;

import br.com.cloudport.serviconavio.estiva.entidade.AtribuicaoEstiva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtribuicaoEstivaRepositorio extends JpaRepository<AtribuicaoEstiva, Long> {
}
