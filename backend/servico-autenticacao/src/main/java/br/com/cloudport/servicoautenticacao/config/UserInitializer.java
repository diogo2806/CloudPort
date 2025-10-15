package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import br.com.cloudport.servicoautenticacao.repositories.PapelRepositorio;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PapelRepositorio papelRepositorio;

    public UserInitializer(UsuarioRepositorio usuarioRepositorio, PapelRepositorio papelRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.papelRepositorio = papelRepositorio;
    }

    @Override
    public void run(String... args) {
        String loginAdministrador = "gitpod";
        String senhaAdministrador = new BCryptPasswordEncoder().encode("gitpod");

        Papel papelAdministrador = garantirPapel("ROLE_ADMIN_PORTO");
        garantirPapel("ROLE_PLANEJADOR");
        garantirPapel("ROLE_OPERADOR_GATE");
        garantirPapel("ROLE_TRANSPORTADORA");

        if (usuarioRepositorio.findByLogin(loginAdministrador).isEmpty()) {
            Usuario administrador = new Usuario(loginAdministrador, senhaAdministrador, "Administrador CloudPort",
                    null, null, new HashSet<>());

            Set<UsuarioPapel> papeis = administrador.getPapeis();
            UsuarioPapel papelUsuarioAdministrador = new UsuarioPapel(administrador, papelAdministrador);
            papeis.add(papelUsuarioAdministrador);

            usuarioRepositorio.save(administrador);
        }
    }

    private Papel garantirPapel(String identificadorPapel) {
        return papelRepositorio.findByNome(identificadorPapel)
                .orElseGet(() -> {
                    Papel papel = new Papel(identificadorPapel);
                    return papelRepositorio.save(papel);
                });
    }
}
