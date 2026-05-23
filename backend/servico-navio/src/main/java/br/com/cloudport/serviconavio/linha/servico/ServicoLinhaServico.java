package br.com.cloudport.serviconavio.linha.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.linha.dto.AtualizacaoServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.dto.CadastroServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.dto.PortoRotacaoDTO;
import br.com.cloudport.serviconavio.linha.dto.PortoRotacaoRequest;
import br.com.cloudport.serviconavio.linha.dto.ServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.entidade.PortoRotacao;
import br.com.cloudport.serviconavio.linha.entidade.ServicoLinha;
import br.com.cloudport.serviconavio.linha.repositorio.ServicoLinhaRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoLinhaServico {

    private final ServicoLinhaRepositorio servicoLinhaRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ServicoLinhaServico(ServicoLinhaRepositorio servicoLinhaRepositorio,
                               SanitizadorEntrada sanitizadorEntrada) {
        this.servicoLinhaRepositorio = servicoLinhaRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<ServicoLinhaDTO> listar() {
        return servicoLinhaRepositorio.findAllByOrderByCodigoAsc().stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicoLinhaDTO buscar(Long identificador) {
        return mapear(obter(identificador));
    }

    public ServicoLinha obter(Long identificador) {
        return servicoLinhaRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço de linha não encontrado."));
    }

    @Transactional
    public ServicoLinhaDTO registrar(CadastroServicoLinhaDTO dto) {
        String codigo = sanitizadorEntrada.limparTextoObrigatorio(dto.getCodigo(), "código do serviço").toUpperCase(Locale.ROOT);
        if (servicoLinhaRepositorio.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Já existe um serviço de linha com o código informado.");
        }
        ServicoLinha servico = new ServicoLinha();
        servico.setCodigo(codigo);
        servico.setNome(sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do serviço"));
        servico.setArmador(sanitizadorEntrada.limparTexto(dto.getArmador()));
        aplicarRotacao(servico, dto.getRotacao());
        return mapear(servicoLinhaRepositorio.save(servico));
    }

    @Transactional
    public ServicoLinhaDTO atualizar(Long identificador, AtualizacaoServicoLinhaDTO dto) {
        ServicoLinha servico = obter(identificador);
        if (StringUtils.hasText(dto.getNome())) {
            servico.setNome(sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do serviço"));
        }
        if (dto.getArmador() != null) {
            servico.setArmador(sanitizadorEntrada.limparTexto(dto.getArmador()));
        }
        if (dto.getRotacao() != null) {
            servico.limparRotacao();
            aplicarRotacao(servico, dto.getRotacao());
        }
        return mapear(servicoLinhaRepositorio.save(servico));
    }

    @Transactional
    public void remover(Long identificador) {
        servicoLinhaRepositorio.delete(obter(identificador));
    }

    private void aplicarRotacao(ServicoLinha servico, List<PortoRotacaoRequest> portos) {
        if (portos == null) {
            return;
        }
        for (PortoRotacaoRequest request : portos) {
            PortoRotacao porto = new PortoRotacao();
            porto.setSequencia(request.getSequencia());
            porto.setPortoUnloc(sanitizadorEntrada
                    .limparTextoObrigatorio(request.getPortoUnloc(), "porto da rotação")
                    .toUpperCase(Locale.ROOT));
            porto.setNomePorto(sanitizadorEntrada.limparTexto(request.getNomePorto()));
            servico.adicionarPorto(porto);
        }
    }

    private ServicoLinhaDTO mapear(ServicoLinha servico) {
        List<PortoRotacaoDTO> rotacao = servico.getRotacao().stream()
                .map(porto -> new PortoRotacaoDTO(
                        porto.getIdentificador(),
                        porto.getSequencia(),
                        porto.getPortoUnloc(),
                        porto.getNomePorto()))
                .collect(Collectors.toList());
        return new ServicoLinhaDTO(
                servico.getIdentificador(),
                servico.getCodigo(),
                servico.getNome(),
                servico.getArmador(),
                rotacao
        );
    }
}
