package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.AtualizacaoConteinerDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerDetalheDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerResumoDTO;
import br.com.cloudport.servicoyard.container.dto.HistoricoOperacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroAlocacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroInspecaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroLiberacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroTransferenciaDTO;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConteinerServico {

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final MovimentoPatioRepositorio movimentoRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ConteinerServico(ConteinerPatioRepositorio conteinerRepositorio,
                            MovimentoPatioRepositorio movimentoRepositorio,
                            PosicaoPatioRepositorio posicaoRepositorio,
                            SanitizadorEntrada sanitizadorEntrada) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.movimentoRepositorio = movimentoRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<ConteinerResumoDTO> listarResumo() {
        return conteinerRepositorio
                .findAll(Sort.by(Sort.Direction.ASC, "codigo"))
                .stream()
                .map(c -> new ConteinerResumoDTO(
                        c.getId(),
                        c.getCodigo(),
                        formatarPosicao(c),
                        c.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConteinerDetalheDTO buscarDetalhe(Long id) {
        return mapearDetalhe(localizar(id));
    }

    @Transactional(readOnly = true)
    public ConteinerDetalheDTO buscarDetalhePorCodigo(String codigo) {
        return mapearDetalhe(localizarPorCodigo(codigo));
    }

    @Transactional
    public ConteinerDetalheDTO registrarAlocacao(RegistroAlocacaoDTO dto) {
        String codigo = limparObrigatorio(dto.getIdentificacao(), "Identificação inválida")
                .toUpperCase(Locale.ROOT);
        validarCodigoUnico(codigo);

        PosicaoPatio posicao = resolverPosicao(dto.getPosicaoPatio());

        ConteinerPatio c = new ConteinerPatio();
        c.setCodigo(codigo);
        c.setPosicao(posicao);
        c.setTipoCarga(validarTipoCarga(dto.getTipoCarga()));
        c.setPesoToneladas(validarPeso(dto.getPesoToneladas()));
        c.setRestricoes(sanitizadorEntrada.limparTexto(dto.getRestricoes()));
        c.setStatus(StatusConteiner.ALOCADO);
        c.setDestino(posicao.getCamadaOperacional());

        ConteinerPatio salvo = conteinerRepositorio.save(c);
        registrarMovimento(salvo, TipoMovimentoPatio.ALOCACAO,
                "Alocação registrada", null, formatarPosicao(salvo), null);
        return mapearDetalhe(salvo);
    }

    @Transactional
    public ConteinerDetalheDTO atualizarCadastro(Long id, AtualizacaoConteinerDTO dto) {
        ConteinerPatio c = localizar(id);
        garantirNaoFinalizado(c, "Contêiner finalizado não permite atualização cadastral");

        PosicaoPatio posicao = resolverPosicao(dto.getPosicaoPatio());
        String posAnterior = formatarPosicao(c);

        c.setPosicao(posicao);
        c.setTipoCarga(validarTipoCarga(dto.getTipoCarga()));
        c.setPesoToneladas(validarPeso(dto.getPesoToneladas()));
        c.setRestricoes(sanitizadorEntrada.limparTexto(dto.getRestricoes()));

        ConteinerPatio salvo = conteinerRepositorio.save(c);
        registrarMovimento(salvo, TipoMovimentoPatio.ATUALIZACAO_CADASTRAL,
                "Dados operacionais atualizados", posAnterior, formatarPosicao(salvo), null);
        return mapearDetalhe(salvo);
    }

    @Transactional
    public ConteinerDetalheDTO registrarTransferencia(Long id, RegistroTransferenciaDTO dto) {
        ConteinerPatio c = localizar(id);
        garantirNaoFinalizado(c, "Contêiner finalizado não pode ser transferido");

        String destino = limparObrigatorio(dto.getPosicaoDestino(), "Posição de destino inválida");
        String motivo = limparObrigatorio(dto.getMotivo(), "Motivo inválido");
        String posAnterior = formatarPosicao(c);

        PosicaoPatio novaPosicao = resolverPosicao(destino);
        c.setPosicao(novaPosicao);
        c.setStatus(StatusConteiner.ALOCADO);

        ConteinerPatio salvo = conteinerRepositorio.save(c);
        registrarMovimento(salvo, TipoMovimentoPatio.TRANSFERENCIA,
                "Transferência: " + destino + " - " + motivo,
                posAnterior, formatarPosicao(salvo),
                sanitizadorEntrada.limparTexto(dto.getResponsavel()));
        return mapearDetalhe(salvo);
    }

    @Transactional
    public ConteinerDetalheDTO registrarInspecao(Long id, RegistroInspecaoDTO dto) {
        ConteinerPatio c = localizar(id);
        garantirNaoFinalizado(c, "Contêiner finalizado não pode ser inspecionado");

        c.setStatus(StatusConteiner.INSPECIONADO);
        ConteinerPatio salvo = conteinerRepositorio.save(c);

        String resultado = limparObrigatorio(dto.getResultado(), "Resultado inválido");
        String obs = sanitizadorEntrada.limparTexto(dto.getObservacoes());
        String descricao = obs != null ? "Inspeção: " + resultado + " - " + obs : "Inspeção: " + resultado;

        registrarMovimento(salvo, TipoMovimentoPatio.INSPECAO, descricao,
                formatarPosicao(salvo), formatarPosicao(salvo),
                sanitizadorEntrada.limparTexto(dto.getResponsavel()));
        return mapearDetalhe(salvo);
    }

    @Transactional
    public ConteinerDetalheDTO registrarLiberacao(Long id, RegistroLiberacaoDTO dto) {
        ConteinerPatio c = localizar(id);
        if (c.getStatus() == StatusConteiner.LIBERADO || c.getStatus() == StatusConteiner.DESPACHADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contêiner já está liberado");
        }
        c.setStatus(StatusConteiner.LIBERADO);
        ConteinerPatio salvo = conteinerRepositorio.save(c);

        String destinoFinal = limparObrigatorio(dto.getDestinoFinal(), "Destino inválido");
        String obs = sanitizadorEntrada.limparTexto(dto.getObservacoes());
        String descricao = obs != null ? "Liberação para " + destinoFinal + " - " + obs
                                       : "Liberação para " + destinoFinal;

        registrarMovimento(salvo, TipoMovimentoPatio.LIBERACAO, descricao,
                formatarPosicao(salvo), formatarPosicao(salvo),
                sanitizadorEntrada.limparTexto(dto.getResponsavel()));
        return mapearDetalhe(salvo);
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacaoDTO> consultarHistorico(Long id) {
        ConteinerPatio c = localizar(id);
        return movimentoRepositorio.findByConteinerIdOrderByRegistradoEmDesc(c.getId())
                .stream()
                .map(m -> new HistoricoOperacaoDTO(
                        m.getTipoMovimento(), m.getDescricao(),
                        m.getPosicaoAnterior(), m.getPosicaoAtual(),
                        m.getResponsavel(), m.getRegistradoEm()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacaoDTO> consultarHistoricoPorCodigo(String codigo) {
        ConteinerPatio c = localizarPorCodigo(codigo);
        return movimentoRepositorio.findByConteinerIdOrderByRegistradoEmDesc(c.getId())
                .stream()
                .map(m -> new HistoricoOperacaoDTO(
                        m.getTipoMovimento(), m.getDescricao(),
                        m.getPosicaoAnterior(), m.getPosicaoAtual(),
                        m.getResponsavel(), m.getRegistradoEm()))
                .collect(Collectors.toList());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ConteinerPatio localizar(Long id) {
        return conteinerRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contêiner não encontrado"));
    }

    private ConteinerPatio localizarPorCodigo(String codigo) {
        String limpo = sanitizadorEntrada.limparTexto(codigo);
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código do contêiner inválido");
        }
        return conteinerRepositorio.findByCodigoIgnoreCase(limpo.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contêiner não encontrado"));
    }

    private void validarCodigoUnico(String codigo) {
        conteinerRepositorio.findByCodigoIgnoreCase(codigo).ifPresent(c -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Código já cadastrado");
        });
    }

    private String limparObrigatorio(String texto, String mensagem) {
        String limpo = sanitizadorEntrada.limparTexto(texto);
        if (limpo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
        return limpo;
    }

    private BigDecimal validarPeso(BigDecimal peso) {
        if (peso == null || peso.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peso inválido");
        }
        return peso;
    }

    private TipoCargaConteiner validarTipoCarga(TipoCargaConteiner tipoCarga) {
        if (tipoCarga == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de carga inválido");
        }
        return tipoCarga;
    }

    private void garantirNaoFinalizado(ConteinerPatio c, String mensagem) {
        if (c.getStatus() == StatusConteiner.LIBERADO || c.getStatus() == StatusConteiner.DESPACHADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    /**
     * Resolve ou cria um PosicaoPatio a partir de uma string no formato "linha-coluna"
     * ou "linha-coluna-camada". Se o formato não for reconhecido, usa coluna=0 e camada "1".
     */
    private PosicaoPatio resolverPosicao(String posicaoPatio) {
        if (!StringUtils.hasText(posicaoPatio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Posição inválida");
        }
        String[] partes = posicaoPatio.split("[-_/]");
        int linha = 0;
        int coluna = 0;
        String camada = "1";
        try {
            if (partes.length >= 2) {
                linha = Integer.parseInt(partes[0].trim());
                coluna = Integer.parseInt(partes[1].trim());
            }
            if (partes.length >= 3) {
                camada = partes[2].trim();
            }
        } catch (NumberFormatException ignored) {
            camada = posicaoPatio;
        }
        final int l = linha;
        final int c = coluna;
        final String cam = camada;
        return posicaoRepositorio.findByLinhaAndColunaAndCamadaOperacional(l, c, cam)
                .orElseGet(() -> {
                    PosicaoPatio nova = new PosicaoPatio();
                    nova.setLinha(l);
                    nova.setColuna(c);
                    nova.setCamadaOperacional(cam);
                    return posicaoRepositorio.save(nova);
                });
    }

    private String formatarPosicao(ConteinerPatio c) {
        if (c.getPosicao() == null) return null;
        PosicaoPatio p = c.getPosicao();
        return p.getLinha() + "-" + p.getColuna() + "-" + p.getCamadaOperacional();
    }

    private ConteinerDetalheDTO mapearDetalhe(ConteinerPatio c) {
        return new ConteinerDetalheDTO(
                c.getId(), c.getCodigo(), formatarPosicao(c),
                c.getTipoCarga(), c.getPesoToneladas(), c.getRestricoes(),
                c.getStatus(), c.getAtualizadoEm());
    }

    private void registrarMovimento(ConteinerPatio conteiner, TipoMovimentoPatio tipo,
                                    String descricao, String posAnterior,
                                    String posAtual, String responsavel) {
        MovimentoPatio m = new MovimentoPatio();
        m.setConteiner(conteiner);
        m.setTipoMovimento(tipo);
        m.setDescricao(descricao != null ? descricao : tipo.name());
        m.setPosicaoAnterior(posAnterior);
        m.setPosicaoAtual(posAtual);
        m.setResponsavel(responsavel);
        m.setRegistradoEm(LocalDateTime.now());
        movimentoRepositorio.save(m);
    }
}
