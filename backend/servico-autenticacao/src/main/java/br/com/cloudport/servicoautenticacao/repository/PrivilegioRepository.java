package br.com.cloudport.servicoautenticacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.cloudport.servicoautenticacao.model.Privilegio;

public interface PrivilegioRepository extends JpaRepository<Privilegio, Long> {
    // Você pode adicionar métodos personalizados aqui, se necessário
}
