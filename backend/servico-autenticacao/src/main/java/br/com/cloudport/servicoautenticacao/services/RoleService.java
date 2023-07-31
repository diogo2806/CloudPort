package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.dto.RoleDTO;
import br.com.cloudport.servicoautenticacao.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public RoleDTO saveRole(RoleDTO roleDTO) {
        Role role = new Role(roleDTO.getName()); // Converte o DTO para a entidade
        Role savedRole = roleRepository.save(role);
        return new RoleDTO(savedRole.getName()); // Converte a entidade para o DTO
    }

    public RoleDTO findByName(String name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role " + name + " not found"));
        return new RoleDTO(role.getName()); // Converte a entidade para o DTO
    }
}
