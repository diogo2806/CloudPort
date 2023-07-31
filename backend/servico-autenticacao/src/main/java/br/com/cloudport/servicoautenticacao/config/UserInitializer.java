package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import br.com.cloudport.servicoautenticacao.repositories.RoleRepository;
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
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        String adminLogin = "gitpod";
        String adminPassword = new BCryptPasswordEncoder().encode("gitpod");

        Role adminRole = roleRepository.findByName("ADMIN")
                             .orElseGet(() -> {
                                 Role newAdminRole = new Role("ADMIN");
                                 roleRepository.save(newAdminRole);
                                 return newAdminRole;
                             });

        Set<UserRole> roles = new HashSet<>();
        roles.add(new UserRole(adminRole));

        if (!userRepository.findByLogin(adminLogin).isPresent()) {
            User adminUser = new User(adminLogin, adminPassword, roles);
            userRepository.save(adminUser);

            // Set user to each UserRole and save
            for (UserRole userRole : roles) {
                userRole.setUser(adminUser);
            }
        }
    }
}
