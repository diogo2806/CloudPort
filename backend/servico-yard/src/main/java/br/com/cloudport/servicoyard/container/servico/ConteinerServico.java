package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.AtualizacaoConteinerDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerDetalheDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerResumoDTO;
import br.com.cloudport.servicoyard.container.dto.HistoricoOperacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroAlocacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroInspecaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroLiberacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroTransferenciaDTO;
import br.com.cloudport.servicoyard.container.entidade.Conteiner;
import br.com.cloudport.servicoyard.container.entidade.HistoricoOperacaoConteiner;
import br.com.cloudport.servicoyard.container.entidade.StatusOperacionalConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoOperacaoConteiner;
import br.com.cloudport.servicoyard.container.repositorio.ConteinerRepositorio;
import br.com.cloudport.servicoyard.container.repositorio.HistoricoOperacaoConteinerRepositorio;
import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConteinerServico {
    private final ConteinerRepositorio conteinerRepositorio;
    private final HistoricoOperacaoConteinerRepositorio historicoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ConteinerServico(ConteinerRepositorio conteinerRepositorio,
                            HistoricoOperacaoConteinerRepositorio historicoRepositorio,
                            SanitizadorEntrada sanitizadorEntrada) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<ConteinerResumoDTO> listarResumo() {
        return conteinerRepositorio.findAll(Sort.by(Sort.Direction.ASC, "identificacao")).stream()
                .map(conteiner -> new ConteinerResumoDTO(
                        conteiner.getId(),
                        conteiner.getIdentificacao(),
                        conteiner.getPosicaoPatio(),
                        conteiner.getStatusOperacional()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConteinerDetalheDTO buscarDetalhe(Long identificador) {
        Conteiner conteiner = localizarConteiner(identificador);
        return mapearDetalhe(conteiner);
    }

    @Transactional
    public ConteinerDetalheDTO registrarAlocacao(RegistroAlocacaoDTO dto) {
        String identificacao = limparObrigatorio(dto.getIdentificacao(), "Identificação inválida");
        validarIdentificacaoUnica(identificacao);
        Conteiner conteiner = new Conteiner();
        conteiner.setIdentificacao(identificacao);
        conteiner.setPosicaoPatio(limparObrigatorio(dto.getPosicaoPatio(), "Posição inválida"));
        conteiner.setTipoCarga(validarTipoCarga(dto.getTipoCarga()));
        conteiner.setPesoToneladas(validarPeso(dto.getPesoToneladas()));
        conteiner.setRestricoes(sanitizadorEntrada.limparTexto(dto.getRestricoes()));
        conteiner.setStatusOperacional(StatusOperacionalConteiner.ALOCADO);
        Conteiner salvo = conteinerRepositorio.save(conteiner);
        registrarHistorico(salvo, TipoOperacaoConteiner.ALOCACAO,
                "Alocação registrada para o contêiner", null,
                salvo.getPosicaoPatio(), null);
        return mapearDetalhe(salvo);
    }

    @Transactional
    public ConteinerDetalheDTO atualizarCadastro(Long identificador, AtualizacaoConteinerDTO dto) {
        Conteiner conteiner = localizarConteiner(identificador);
        if (conteiner.getStatusOperacional() == StatusOperacionalConteiner.LIBERADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contêiner liberado não permite atualização cadastral");
        }
        String posicao = limparObrigatorio(dto.getPosicaoPatio(), "Posição inválida");
        BigDecimal peso = validarPeso(dto.getPesoToneladas());
        conteiner.setPosicaoPatio(posicao);
        conteiner.setTipoCarga(validarTipoCarga(dto.getTipoCarga()));
        conteiner.setPesoToneladas(peso);
        conteiner.setRestricoes(sanitizadorEntrada.limparTexto(dto.getRestricoes()));
        Conteiner atualizado = conteinerRepositorio.save(conteiner);
        registrarHistorico(atualizado, TipoOperacaoConteiner.ATUALIZACAO_CADASTRAL,
                "Dados operacionais atualizados", null,
                atualizado.getPosicaoPatio(), null);
        return mapearDetalhe(atualizado);
    }

    @Transactional
    public ConteinerDetalheDTO registrarTransferencia(Long identificador, RegistroTransferenciaDTO dto) {
        Conteiner conteiner = localizarConteiner(identificador);
        garantirNaoLiberado(conteiner, "Contêiner liberado não pode ser transferido");
        String destino = limparObrigatorio(dto.getPosicaoDestino(), "Posição de destino inválida");
        String motivo = limparObrigatorio(dto.getMotivo(), "Motivo inválido");
        String posicaoAnterior = conteiner.getPosicaoPatio();
        conteiner.setPosicaoPatio(destino);
        conteiner.setStatusOperacional(StatusOperacionalConteiner.ALOCADO);
        Conteiner atualizado = conteinerRepositorio.save(conteiner);
        String descricao = "Transferência registrada: " + destino + " - " + motivo;
        registrarHistorico(atualizado, TipoOperacaoConteiner.TRANSFERENCIA, descricao,
                posicaoAnterior, destino, sanitizadorEntrada.limparTexto(dto.getResponsavel()));
        return mapearDetalhe(atualizado);
    }

    @Transactional
    public ConteinerDetalheDTO registrarInspecao(Long identificador, RegistroInspecaoDTO dto) {
        Conteiner conteiner = localizarConteiner(identificador);
        garantirNaoLiberado(conteiner, "Contêiner liberado não pode ser inspecionado");
        conteiner.setStatusOperacional(StatusOperacionalConteiner.INSPECIONADO);
        Conteiner atualizado = conteinerRepositorio.save(conteiner);
        String resultado = limparObrigatorio(dto.getResultado(), "Resultado inválido");
        String descricao = "Inspeção realizada: " + resultado;
        String observacoes = sanitizadorEntrada.limparTexto(dto.getObservacoes());
        if (observacoes != null) {
            descricao = descricao + " - Observações: " + observacoes;
        }
        registrarHistorico(atualizado, TipoOperacaoConteiner.INSPECAO, descricao,
                atualizado.getPosicaoPatio(), atualizado.getPosicaoPatio(),
                sanitizadorEntrada.limparTexto(dto.getResponsavel()));
        return mapearDetalhe(atualizado);
    }

    @Transactional
    public ConteinerDetalheDTO registrarLiberacao(Long identificador, RegistroLiberacaoDTO dto) {
        Conteiner conteiner = localizarConteiner(identificador);
        garantirNaoLiberado(conteiner, "Contêiner já está liberado");
        conteiner.setStatusOperacional(StatusOperacionalConteiner.LIBERADO);
        Conteiner atualizado = conteinerRepositorio.save(conteiner);
        String destinoFinal = limparObrigatorio(dto.getDestinoFinal(), "Destino inválido");
        String descricao = "Liberação para " + destinoFinal;
        String observacoes = sanitizadorEntrada.limparTexto(dto.getObservacoes());
        if (observacoes != null) {
            descricao = descricao + " - Observações: " + observacoes;
        }
        registrarHistorico(atualizado, TipoOperacaoConteiner.LIBERACAO, descricao,
                atualizado.getPosicaoPatio(), atualizado.getPosicaoPatio(),
                sanitizadorEntrada.limparTexto(dto.getResponsavel()));
        return mapearDetalhe(atualizado);
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacaoDTO> consultarHistorico(Long identificador) {
        Conteiner conteiner = localizarConteiner(identificador);
        return historicoRepositorio.findByConteinerIdOrderByDataRegistroDesc(conteiner.getId()).stream()
                .map(registro -> new HistoricoOperacaoDTO(
                        registro.getTipoOperacao(),
                        registro.getDescricao(),
                        registro.getPosicaoAnterior(),
                        registro.getPosicaoAtual(),
                        registro.getResponsavel(),
                        registro.getDataRegistro()))
                .collect(Collectors.toList());
    }

    private Conteiner localizarConteiner(Long identificador) {
        return conteinerRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contêiner não encontrado"));
    }

    private void validarIdentificacaoUnica(String identificacao) {
        conteinerRepositorio.findByIdentificacaoIgnoreCase(identificacao)
                .ifPresent(c -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Identificação já cadastrada");
                });
    }

    private String limparObrigatorio(String texto, String mensagemErro) {
        String limpo = sanitizadorEntrada.limparTexto(texto);
        if (limpo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagemErro);
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

    private void garantirNaoLiberado(Conteiner conteiner, String mensagem) {
        if (conteiner.getStatusOperacional() == StatusOperacionalConteiner.LIBERADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    private ConteinerDetalheDTO mapearDetalhe(Conteiner conteiner) {
        return new ConteinerDetalheDTO(
                conteiner.getId(),
                conteiner.getIdentificacao(),
                conteiner.getPosicaoPatio(),
                conteiner.getTipoCarga(),
                conteiner.getPesoToneladas(),
                conteiner.getRestricoes(),
                conteiner.getStatusOperacional(),
                conteiner.getUltimaAtualizacao());
    }

    private void registrarHistorico(Conteiner conteiner, TipoOperacaoConteiner tipoOperacao,
                                    String descricao, String posicaoAnterior,
                                    String posicaoAtual, String responsavel) {
        HistoricoOperacaoConteiner historico = new HistoricoOperacaoConteiner();
        historico.setConteiner(conteiner);
        historico.setTipoOperacao(tipoOperacao);
        historico.setDescricao(limparObrigatorio(descricao, "Descrição inválida"));
        historico.setPosicaoAnterior(sanitizadorEntrada.limparTexto(posicaoAnterior));
        historico.setPosicaoAtual(sanitizadorEntrada.limparTexto(posicaoAtual));
        historico.setResponsavel(sanitizadorEntrada.limparTexto(responsavel));
        historico.setDataRegistro(OffsetDateTime.now(ZoneOffset.UTC));
        historicoRepositorio.save(historico);
    }
}
