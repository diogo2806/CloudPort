package br.com.cloudport.servicoautenticacao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import br.com.cloudport.servicoautenticacao.model.Privilegio;
import br.com.cloudport.servicoautenticacao.repository.PrivilegioRepository;

import java.util.List;

@Service
public class PrivilegioService {

    private final PrivilegioRepository privilegioRepository;

    @Autowired
    public PrivilegioService(PrivilegioRepository privilegioRepository) {
        this.privilegioRepository = privilegioRepository;
    }

    public List<Privilegio> getAllPrivilegios() {
        return privilegioRepository.findAll();
    }

    public Privilegio getPrivilegioById(Long id) {
        return privilegioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Privilegio not found"));
    }

    public Privilegio createPrivilegio(Privilegio privilegio) {
        return privilegioRepository.save(privilegio);
    }

    public Privilegio updatePrivilegio(Privilegio privilegio) {
        return privilegioRepository.save(privilegio);
    }

    public void deletePrivilegio(Long id) {
        privilegioRepository.deleteById(id);
    }

    // Você pode adicionar mais métodos aqui, se necessário
}
