package br.com.cloudport.servicoautenticacao.repositorio;

import br.com.cloudport.servicoautenticacao.modelo.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaRepositorio extends JpaRepository<Empresa, Long> {
    // Aqui você pode adicionar métodos personalizados para consultas, se necessário
    Empresa findByCnpj(String cnpj);

}
