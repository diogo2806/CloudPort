package br.com.cloudport.servicoautenticacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cloudport.servicoautenticacao.model.Contato;

import java.util.List;

@Repository
public interface ContatoRepository extends JpaRepository<Contato, Long> {
    // Aqui você pode adicionar métodos personalizados para consultas se necessário
    List<Contato> findByNome(String nome);
}

