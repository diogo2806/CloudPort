package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.app.administracao.UserRepository;
import br.com.cloudport.servicoautenticacao.app.administracao.RoleRepository;
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

        Role adminRole = ensureRoleExists("ROLE_ADMIN_PORTO");
        ensureRoleExists("ROLE_PLANEJADOR");
        ensureRoleExists("ROLE_OPERADOR_GATE");
        ensureRoleExists("ROLE_TRANSPORTADORA");

        if (!userRepository.findByLogin(adminLogin).isPresent()) {
            User adminUser = new User(adminLogin, adminPassword, "Administrador CloudPort",
                    null, null, new HashSet<>());

            Set<UserRole> roles = adminUser.getRoles();
            UserRole adminUserRole = new UserRole(adminUser, adminRole);
            roles.add(adminUserRole);

            userRepository.save(adminUser);
        }
    }

    private Role ensureRoleExists(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role(roleName);
                    return roleRepository.save(role);
                });
    }
}
