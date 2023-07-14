package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoRepositorio extends JpaRepository<Documento, Long> {

    // Aqui você pode adicionar métodos personalizados de consulta, se necessário.

}
