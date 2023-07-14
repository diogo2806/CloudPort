package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContatoRepositorio extends JpaRepository<Contato, Long> {
    // Aqui você pode adicionar métodos personalizados para consultas se necessário
    List<Contato> findByNome(String nome);
}

