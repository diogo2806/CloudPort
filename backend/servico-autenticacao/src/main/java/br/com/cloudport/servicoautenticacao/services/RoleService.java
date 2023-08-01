package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.dto.RoleDTO;
import br.com.cloudport.servicoautenticacao.repositories.RoleRepository;
import br.com.cloudport.servicoautenticacao.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository; // Declare userRoleRepository aqui

    @Autowired
    public RoleService(RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository; // Injete userRoleRepository aqui
    }

    public RoleDTO saveRole(RoleDTO roleDTO) {
        if(roleDTO.getName() == null || roleDTO.getName().trim().isEmpty()){
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        
        // Verifica se já existe um role com o mesmo nome
        roleRepository.findByName(roleDTO.getName()).ifPresent(role -> {
            throw new IllegalArgumentException("Role with name " + roleDTO.getName() + " already exists");
        });

        Role role = new Role(roleDTO.getName()); // Converte o DTO para a entidade
        Role savedRole = roleRepository.save(role);
        return new RoleDTO(savedRole.getId(), savedRole.getName()); // Adicione o id aqui
    }
    

    public RoleDTO findByName(String name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role " + name + " not found"));
        return new RoleDTO(role.getId(), role.getName()); // Adicione o id aqui
    }

    public List<RoleDTO> findAll() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .map(role -> new RoleDTO(role.getId(), role.getName())) 
                .sorted(Comparator.comparing(RoleDTO::getId)) // Adiciona a ordenação aqui
                .collect(Collectors.toList());
    }
    

    public RoleDTO updateRole(Long id, RoleDTO roleDTO) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role with id " + id + " not found"));
        role.setName(roleDTO.getName()); // Atualiza o nome da função
        Role updatedRole = roleRepository.save(role);
        return new RoleDTO(updatedRole.getId(), updatedRole.getName()); // Retorna a função atualizada
    }
    
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Role com id " + id + " não encontrada"));
        
        // Verifique se há UserRoles associados a essa 'role'
        long count = userRoleRepository.countByRoleId(id);
        if (count > 0) {
            throw new IllegalStateException("Não é possível deletar a role com id " + id + ", pois ela ainda está atribuída a usuários");
        }
        
        roleRepository.delete(role); // Deleta a 'role'
    }
    
}
