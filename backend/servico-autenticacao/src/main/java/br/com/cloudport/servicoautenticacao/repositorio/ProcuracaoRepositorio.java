package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.Procuracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcuracaoRepositorio extends JpaRepository<Procuracao, Long> {
    // Aqui você pode adicionar métodos personalizados de consulta, se necessário.
}
