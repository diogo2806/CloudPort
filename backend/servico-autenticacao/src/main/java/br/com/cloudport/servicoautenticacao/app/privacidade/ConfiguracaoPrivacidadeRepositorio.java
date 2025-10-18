package br.com.cloudport.servicoautenticacao.app.privacidade;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoPrivacidadeRepositorio extends JpaRepository<ConfiguracaoPrivacidade, UUID> {
}
