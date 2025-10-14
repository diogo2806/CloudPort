package br.com.cloudport.servicoautenticacao.services;

import br.com.cloudport.servicoautenticacao.dto.UserSummaryDTO;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserSummaryDTO> listarUsuariosResumo() {
        return userRepository.findAll()
                .stream()
                .map(UserSummaryDTO::fromUser)
                .collect(Collectors.toList());
    }
}
