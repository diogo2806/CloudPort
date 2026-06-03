package br.com.cloudport.servicoyard.estivagembulk.repositorio;

import br.com.cloudport.servicoyard.estivagembulk.modelo.ItemCargoSiderurgico;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoCargaSiderurgica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCargoSiderurgicoRepositorio extends JpaRepository<ItemCargoSiderurgico, Long> {

    List<ItemCargoSiderurgico> findByPlanoId(Long planoId);

    List<ItemCargoSiderurgico> findByPlanoIdAndTipoCarga(Long planoId, TipoCargaSiderurgica tipoCarga);

    List<ItemCargoSiderurgico> findByPlanoIdAndPortoDescarga(Long planoId, String portoDescarga);
}
