package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.model.StatusEnum;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void findByNameLoadsRoleWithRelatedUserRoles() {
        Role role = roleRepository.save(new Role("ADMIN"));

        User user = new User(UUID.randomUUID(), "john.doe", "secret", new HashSet<>());
        userRepository.save(user);

        UserRole relation = new UserRole(user, role);
        relation.setStatusEnum(StatusEnum.ACTIVE);
        user.getRoles().add(relation);
        role.getUserRoles().add(relation);
        userRoleRepository.save(relation);

        Role persisted = roleRepository.findByName("ADMIN").orElseThrow();

        assertThat(persisted.getName()).isEqualTo("ADMIN");
        assertThat(persisted.getUserRoles())
                .hasSize(1)
                .allSatisfy(userRole -> {
                    assertThat(userRole.getUser().getId()).isEqualTo(user.getId());
                    assertThat(userRole.getRole().getId()).isEqualTo(role.getId());
                    assertThat(userRole.getStatusEnum()).isEqualTo(StatusEnum.ACTIVE);
                });
    }
}
