package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavioSiderurgicoRepositorio extends JpaRepository<NavioSiderurgico, Long> {
    boolean existsByCodigoImoIgnoreCase(String codigoImo);
}
