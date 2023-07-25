package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.domain.user.User;
import br.com.cloudport.servicoautenticacao.domain.user.UserRole;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        String adminLogin = "gitpod";
        String adminPassword = new BCryptPasswordEncoder().encode("gitpod");
        UserRole adminRole = UserRole.ADMIN;  // altere para o seu tipo de UserRole para ADMIN

        if (userRepository.findByLogin(adminLogin) == null) {
            User adminUser = new User(adminLogin, adminPassword, adminRole);
            userRepository.save(adminUser);
        }
    }
}
