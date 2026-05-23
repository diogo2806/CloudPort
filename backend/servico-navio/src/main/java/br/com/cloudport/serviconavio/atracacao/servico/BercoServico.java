package br.com.cloudport.serviconavio.atracacao.servico;

import br.com.cloudport.serviconavio.atracacao.dto.AtualizacaoBercoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.BercoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.CadastroBercoDTO;
import br.com.cloudport.serviconavio.atracacao.entidade.Berco;
import br.com.cloudport.serviconavio.atracacao.entidade.StatusBerco;
import br.com.cloudport.serviconavio.atracacao.repositorio.BercoRepositorio;
import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BercoServico {

    private final BercoRepositorio bercoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public BercoServico(BercoRepositorio bercoRepositorio, SanitizadorEntrada sanitizadorEntrada) {
        this.bercoRepositorio = bercoRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<BercoDTO> listar() {
        return bercoRepositorio.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BercoDTO buscar(Long identificador) {
        return mapear(obter(identificador));
    }

    @Transactional
    public BercoDTO registrar(CadastroBercoDTO dto) {
        Berco berco = new Berco();
        String nome = sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do berço");
        validarNomeDisponivel(nome, null);
        berco.setNome(nome);
        berco.setComprimentoMetros(dto.getComprimentoMetros());
        berco.setCaladoMaximoMetros(dto.getCaladoMaximoMetros());
        berco.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusBerco.DISPONIVEL);
        return mapear(bercoRepositorio.save(berco));
    }

    @Transactional
    public BercoDTO atualizar(Long identificador, AtualizacaoBercoDTO dto) {
        Berco berco = obter(identificador);
        if (dto.getNome() != null) {
            String nome = sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do berço");
            validarNomeDisponivel(nome, identificador);
            berco.setNome(nome);
        }
        if (dto.getComprimentoMetros() != null) {
            berco.setComprimentoMetros(dto.getComprimentoMetros());
        }
        if (dto.getCaladoMaximoMetros() != null) {
            berco.setCaladoMaximoMetros(dto.getCaladoMaximoMetros());
        }
        if (dto.getStatus() != null) {
            berco.setStatus(dto.getStatus());
        }
        return mapear(bercoRepositorio.save(berco));
    }

    @Transactional
    public void remover(Long identificador) {
        Berco berco = obter(identificador);
        bercoRepositorio.delete(berco);
    }

    Berco obter(Long identificador) {
        return bercoRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Berço não encontrado."));
    }

    private void validarNomeDisponivel(String nome, Long identificadorAtual) {
        boolean duplicado = identificadorAtual == null
                ? bercoRepositorio.existsByNomeIgnoreCase(nome)
                : bercoRepositorio.existsByNomeIgnoreCaseAndIdentificadorNot(nome, identificadorAtual);
        if (duplicado) {
            throw new IllegalArgumentException("Já existe um berço cadastrado com o nome informado.");
        }
    }

    private BercoDTO mapear(Berco berco) {
        return new BercoDTO(
                berco.getIdentificador(),
                berco.getNome(),
                berco.getComprimentoMetros(),
                berco.getCaladoMaximoMetros(),
                berco.getStatus()
        );
    }
}
