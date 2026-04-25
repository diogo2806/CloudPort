package br.com.cloudport.servicoautenticacao.app.navegacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoNavegacaoRepositorio extends JpaRepository<ConfiguracaoNavegacao, UUID> {

    List<ConfiguracaoNavegacao> findAllByOrderByOrdemAsc();
}
