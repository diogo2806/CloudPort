package br.com.cloudport.servicoautenticacao.app.seguranca;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaSegurancaRepositorio extends JpaRepository<PoliticaSeguranca, UUID> {

    List<PoliticaSeguranca> findByAtivoTrue(Sort ordenacao);

    List<PoliticaSeguranca> findByAtivoTrueAndVersao(String versao, Sort ordenacao);
}
