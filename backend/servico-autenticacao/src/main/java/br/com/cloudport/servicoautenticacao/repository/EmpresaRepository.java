package br.com.cloudport.servicoautenticacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cloudport.servicoautenticacao.model.Empresa;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    // Aqui você pode adicionar métodos personalizados para consultas, se necessário
    Empresa findByCnpj(String cnpj);

}
