package br.com.cloudport.serviconavio.navio.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.navio.dto.AtualizacaoNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.CadastroNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.entidade.StatusOperacaoNavio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class NavioServico {

    private final NavioRepositorio navioRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public NavioServico(NavioRepositorio navioRepositorio,
                        SanitizadorEntrada sanitizadorEntrada) {
        this.navioRepositorio = navioRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<NavioResumoDTO> listarResumo() {
        return navioRepositorio.findAll(Sort.by(Sort.Direction.ASC, "dataPrevistaAtracacao")).stream()
                .map(this::mapearResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NavioDetalheDTO buscarDetalhe(Long identificador) {
        Navio navio = obterNavio(identificador);
        return mapearDetalhe(navio);
    }

    @Transactional
    public NavioDetalheDTO registrar(CadastroNavioDTO dto) {
        Navio navio = new Navio();
        preencherDadosObrigatorios(dto, navio);
        navio.setStatusOperacao(StatusOperacaoNavio.AGENDADO);
        navio.setObservacoes(tratarTextoOpcional(dto.getObservacoes()));
        navio.setBercoPrevisto(tratarTextoOpcional(dto.getBercoPrevisto()));
        validarCodigoImoDisponivel(navio.getCodigoImo(), null);
        Navio salvo = navioRepositorio.save(navio);
        return mapearDetalhe(salvo);
    }

    @Transactional
    public NavioDetalheDTO atualizar(Long identificador, AtualizacaoNavioDTO dto) {
        Navio navio = obterNavio(identificador);

        if (StringUtils.hasText(dto.getNome())) {
            navio.setNome(sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do navio"));
        }
        if (StringUtils.hasText(dto.getCodigoImo())) {
            String codigoSanitizado = sanitizadorEntrada.limparTextoObrigatorio(dto.getCodigoImo(), "código IMO").toUpperCase(Locale.ROOT);
            validarCodigoImoDisponivel(codigoSanitizado, navio.getIdentificador());
            navio.setCodigoImo(codigoSanitizado);
        }
        if (StringUtils.hasText(dto.getPaisBandeira())) {
            navio.setPaisBandeira(sanitizadorEntrada.limparTextoObrigatorio(dto.getPaisBandeira(), "país da bandeira"));
        }
        if (StringUtils.hasText(dto.getEmpresaArmadora())) {
            navio.setEmpresaArmadora(sanitizadorEntrada.limparTextoObrigatorio(dto.getEmpresaArmadora(), "empresa armadora"));
        }
        if (dto.getCapacidadeTeu() != null) {
            navio.setCapacidadeTeu(dto.getCapacidadeTeu());
        }
        if (dto.getDataPrevistaAtracacao() != null) {
            navio.setDataPrevistaAtracacao(dto.getDataPrevistaAtracacao());
        }
        if (dto.getDataEfetivaAtracacao() != null) {
            navio.setDataEfetivaAtracacao(dto.getDataEfetivaAtracacao());
        }
        if (dto.getDataEfetivaDesatracacao() != null) {
            navio.setDataEfetivaDesatracacao(dto.getDataEfetivaDesatracacao());
        }
        if (dto.getBercoPrevisto() != null) {
            navio.setBercoPrevisto(tratarTextoOpcional(dto.getBercoPrevisto()));
        }
        if (dto.getBercoAtual() != null) {
            navio.setBercoAtual(tratarTextoOpcional(dto.getBercoAtual()));
        }
        if (dto.getObservacoes() != null) {
            navio.setObservacoes(tratarTextoOpcional(dto.getObservacoes()));
        }
        if (dto.getStatusOperacao() != null) {
            navio.setStatusOperacao(dto.getStatusOperacao());
        }
        Navio salvo = navioRepositorio.save(navio);
        return mapearDetalhe(salvo);
    }

    @Transactional
    public void remover(Long identificador) {
        Navio navio = obterNavio(identificador);
        navioRepositorio.delete(navio);
    }

    private Navio obterNavio(Long identificador) {
        return navioRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Navio não encontrado."));
    }

    private void preencherDadosObrigatorios(CadastroNavioDTO dto, Navio navio) {
        navio.setNome(sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do navio"));
        String codigoImo = sanitizadorEntrada.limparTextoObrigatorio(dto.getCodigoImo(), "código IMO").toUpperCase(Locale.ROOT);
        navio.setCodigoImo(codigoImo);
        navio.setPaisBandeira(sanitizadorEntrada.limparTextoObrigatorio(dto.getPaisBandeira(), "país da bandeira"));
        navio.setEmpresaArmadora(sanitizadorEntrada.limparTextoObrigatorio(dto.getEmpresaArmadora(), "empresa armadora"));
        navio.setCapacidadeTeu(dto.getCapacidadeTeu());
        navio.setDataPrevistaAtracacao(dto.getDataPrevistaAtracacao());
    }

    private void validarCodigoImoDisponivel(String codigoImo, Long identificadorAtual) {
        if (identificadorAtual == null) {
            if (navioRepositorio.existsByCodigoImoIgnoreCase(codigoImo)) {
                throw new IllegalArgumentException("Já existe um navio cadastrado com o código IMO informado.");
            }
        } else if (navioRepositorio.existsByCodigoImoIgnoreCaseAndIdentificadorNot(codigoImo, identificadorAtual)) {
            throw new IllegalArgumentException("Já existe outro navio cadastrado com o código IMO informado.");
        }
    }

    private String tratarTextoOpcional(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        return StringUtils.hasText(limpo) ? limpo : null;
    }

    private NavioResumoDTO mapearResumo(Navio navio) {
        return new NavioResumoDTO(
                navio.getIdentificador(),
                navio.getNome(),
                navio.getCodigoImo(),
                navio.getStatusOperacao(),
                navio.getDataPrevistaAtracacao(),
                navio.getBercoPrevisto()
        );
    }

    private NavioDetalheDTO mapearDetalhe(Navio navio) {
        return new NavioDetalheDTO(
                navio.getIdentificador(),
                navio.getNome(),
                navio.getCodigoImo(),
                navio.getPaisBandeira(),
                navio.getEmpresaArmadora(),
                navio.getCapacidadeTeu(),
                navio.getStatusOperacao(),
                navio.getDataPrevistaAtracacao(),
                navio.getDataEfetivaAtracacao(),
                navio.getDataEfetivaDesatracacao(),
                navio.getBercoPrevisto(),
                navio.getBercoAtual(),
                navio.getObservacoes()
        );
    }
}
