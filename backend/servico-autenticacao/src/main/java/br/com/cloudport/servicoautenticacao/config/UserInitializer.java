package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.repositories.PapelRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private final PapelRepositorio papelRepositorio;

    public UserInitializer(PapelRepositorio papelRepositorio) {
        this.papelRepositorio = papelRepositorio;
    }

    @Override
    public void run(String... args) {
        garantirPapel("ROLE_ADMIN_PORTO");
        garantirPapel("ROLE_PLANEJADOR");
        garantirPapel("ROLE_OPERADOR_GATE");
        garantirPapel("ROLE_TRANSPORTADORA");
    }

    private Papel garantirPapel(String identificadorPapel) {
        return papelRepositorio.findByNome(identificadorPapel)
                .orElseGet(() -> {
                    Papel papel = new Papel(identificadorPapel);
                    return papelRepositorio.save(papel);
                });
    }
}
