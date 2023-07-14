package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Empresa;
import br.com.cloudport.servicoautenticacao.repository.EmpresaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<Empresa> listarTodasEmpresas() {
        return empresaRepository.findAll();
    }

    public Empresa encontrarEmpresaPorId(Long id) {
        return empresaRepository.findById(id).orElse(null);
    }

    public Empresa salvarEmpresa(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    public void deletarEmpresa(Long id) {
        empresaRepository.deleteById(id);
    }
}
