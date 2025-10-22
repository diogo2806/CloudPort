package br.com.cloudport.servicoautenticacao.app.notificacoes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CanalNotificacaoRepositorio extends JpaRepository<CanalNotificacao, Long> {
}
