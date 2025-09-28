package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import br.com.cloudport.servicoautenticacao.repositories.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserInitializerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void initialAdminUserHasPersistedRoles() {
        User adminUser = userRepository.findByLogin("gitpod").orElse(null);
        assertNotNull(adminUser, "Usuário administrador inicial não foi criado");

        assertNotNull(adminUser.getRoles(), "Coleção de papéis não deve ser nula");
        assertFalse(adminUser.getRoles().isEmpty(), "Usuário administrador deve possuir ao menos um papel associado");
        adminUser.getRoles().forEach(userRole -> {
            assertNotNull(userRole.getUser(), "UserRole deve manter referência ao usuário");
            assertEquals(adminUser.getId(), userRole.getUser().getId(), "UserRole deve estar vinculado ao usuário administrador");
        });

        List<UserRole> persistedRoles = userRoleRepository.findAll();
        assertFalse(persistedRoles.isEmpty(), "Deve haver registros de vínculo usuário-papel persistidos");
        persistedRoles.forEach(userRole -> {
            assertNotNull(userRole.getId(), "UserRole deve possuir identificador persistido");
            assertNotNull(userRole.getUser(), "UserRole persistido deve estar vinculado a um usuário");
        });
    }
}
