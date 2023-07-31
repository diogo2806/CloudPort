package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.domain.user.Perfil;
import br.com.cloudport.servicoautenticacao.repositories.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;

    @Autowired
    public PerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    public Perfil savePerfil(Perfil perfil) {
        return perfilRepository.save(perfil);
    }

    public Perfil findByName(String name) {
        return perfilRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Perfil " + name + " not found"));
    }
}
