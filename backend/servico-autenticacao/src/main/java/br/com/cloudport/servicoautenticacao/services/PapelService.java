package br.com.cloudport.servicoautenticacao.services;

import br.com.cloudport.servicoautenticacao.app.administracao.dto.PapelDTO;
import br.com.cloudport.servicoautenticacao.exception.PapelNaoEncontradoException;
import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.repositories.PapelRepositorio;
import br.com.cloudport.servicoautenticacao.repositories.UsuarioPapelRepositorio;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PapelService {

    private final PapelRepositorio papelRepositorio;
    private final UsuarioPapelRepositorio usuarioPapelRepositorio;

    public PapelService(PapelRepositorio papelRepositorio, UsuarioPapelRepositorio usuarioPapelRepositorio) {
        this.papelRepositorio = papelRepositorio;
        this.usuarioPapelRepositorio = usuarioPapelRepositorio;
    }

    @Transactional
    public PapelDTO salvarPapel(PapelDTO papelDTO) {
        if(papelDTO.getNome() == null || papelDTO.getNome().trim().isEmpty()){
            throw new IllegalArgumentException("Nome do papel não pode ser vazio");
        }

        papelRepositorio.findByNome(papelDTO.getNome()).ifPresent(papel -> {
            throw new IllegalArgumentException("Papel com o nome " + papelDTO.getNome() + " já existe");
        });

        Papel papel = new Papel(papelDTO.getNome());
        Papel papelSalvo = papelRepositorio.save(papel);
        return new PapelDTO(papelSalvo.getId(), papelSalvo.getNome());
    }

    @Transactional
    public PapelDTO buscarPorNome(String nome) {
        Papel papel = papelRepositorio.findByNome(nome)
                .orElseThrow(() -> new PapelNaoEncontradoException("Papel " + nome + " não encontrado"));
        return new PapelDTO(papel.getId(), papel.getNome());
    }

    public List<PapelDTO> listarTodos() {
        List<Papel> papeis = papelRepositorio.findAll();
        return papeis.stream()
                .map(papel -> new PapelDTO(papel.getId(), papel.getNome()))
                .sorted(Comparator.comparing(PapelDTO::getId))
                .collect(Collectors.toList());
    }

    @Transactional
    public PapelDTO atualizar(Long id, PapelDTO papelDTO) {
        Papel papel = papelRepositorio.findById(id)
                .orElseThrow(() -> new PapelNaoEncontradoException("Papel com id " + id + " não encontrado"));
        papel.setNome(papelDTO.getNome());
        Papel papelAtualizado = papelRepositorio.save(papel);
        return new PapelDTO(papelAtualizado.getId(), papelAtualizado.getNome());
    }

    @Transactional
    public void remover(Long id) {
        if (usuarioPapelRepositorio.existsByPapelId(id)) {
            throw new IllegalStateException("Papel com id " + id + " não pode ser removido porque está em uso.");
        }

        if (!papelRepositorio.existsById(id)) {
            throw new PapelNaoEncontradoException("Papel com id " + id + " não encontrado.");
        }

        papelRepositorio.deleteById(id);
    }
}
