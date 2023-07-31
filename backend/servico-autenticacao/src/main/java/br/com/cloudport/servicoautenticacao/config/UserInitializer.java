package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.domain.user.User;
import br.com.cloudport.servicoautenticacao.domain.user.Role;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import br.com.cloudport.servicoautenticacao.repositories.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Override
    public void run(String... args) {
        String adminLogin = "gitpod";
        String adminPassword = new BCryptPasswordEncoder().encode("gitpod");

        Role adminperfil = perfilRepository.findByName("ADMIN")
                             .orElseGet(() -> {
                                 Role newAdminPerfil = new Perfil("ADMIN");
                                 perfilRepository.save(newAdminPerfil);
                                 return newAdminPerfil;
                             });

        Set<Role> perfis = new HashSet<>();
        perfis.add(adminperfil);

        if (!userRepository.findByLogin(adminLogin).isPresent()) {
            User adminUser = new User(adminLogin, adminPassword, roles);
            userRepository.save(adminUser);
        }
    }
}
