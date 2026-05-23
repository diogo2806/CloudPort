package br.com.cloudport.serviconavio.atracacao.servico;

import br.com.cloudport.serviconavio.atracacao.dto.AtualizacaoStatusOperacaoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.AtualizacaoVisitaNavioDTO;
import br.com.cloudport.serviconavio.atracacao.dto.CadastroVisitaNavioDTO;
import br.com.cloudport.serviconavio.atracacao.dto.OperacaoNavioConteinerDTO;
import br.com.cloudport.serviconavio.atracacao.dto.OperacaoNavioConteinerRequest;
import br.com.cloudport.serviconavio.atracacao.dto.PlanejamentoAtracacaoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.VisitaNavioDetalheDTO;
import br.com.cloudport.serviconavio.atracacao.dto.VisitaNavioResumoDTO;
import br.com.cloudport.serviconavio.atracacao.entidade.Berco;
import br.com.cloudport.serviconavio.atracacao.entidade.OperacaoNavioConteiner;
import br.com.cloudport.serviconavio.atracacao.entidade.StatusBerco;
import br.com.cloudport.serviconavio.atracacao.entidade.StatusOperacaoNavioConteiner;
import br.com.cloudport.serviconavio.atracacao.entidade.StatusVisitaNavio;
import br.com.cloudport.serviconavio.atracacao.entidade.VisitaNavio;
import br.com.cloudport.serviconavio.atracacao.repositorio.OperacaoNavioConteinerRepositorio;
import br.com.cloudport.serviconavio.atracacao.repositorio.VisitaNavioRepositorio;
import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitaNavioServico {

    private static final Set<StatusVisitaNavio> STATUS_IGNORADOS_CONFLITO =
            EnumSet.of(StatusVisitaNavio.CANCELADA, StatusVisitaNavio.DESATRACADA);

    private final VisitaNavioRepositorio visitaRepositorio;
    private final OperacaoNavioConteinerRepositorio operacaoRepositorio;
    private final NavioRepositorio navioRepositorio;
    private final BercoServico bercoServico;
    private final SanitizadorEntrada sanitizadorEntrada;

    public VisitaNavioServico(VisitaNavioRepositorio visitaRepositorio,
                              OperacaoNavioConteinerRepositorio operacaoRepositorio,
                              NavioRepositorio navioRepositorio,
                              BercoServico bercoServico,
                              SanitizadorEntrada sanitizadorEntrada) {
        this.visitaRepositorio = visitaRepositorio;
        this.operacaoRepositorio = operacaoRepositorio;
        this.navioRepositorio = navioRepositorio;
        this.bercoServico = bercoServico;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<VisitaNavioResumoDTO> listar() {
        return visitaRepositorio.findAllByOrderByAtracacaoPrevistaAsc().stream()
                .map(this::mapearResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VisitaNavioResumoDTO> listarAgendaDeAtracacao() {
        return visitaRepositorio.findByBercoIsNotNullOrderByAtracacaoPrevistaAsc().stream()
                .map(this::mapearResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VisitaNavioDetalheDTO buscarDetalhe(Long identificador) {
        return mapearDetalhe(obter(identificador));
    }

    @Transactional
    public VisitaNavioDetalheDTO registrar(CadastroVisitaNavioDTO dto) {
        Navio navio = navioRepositorio.findById(dto.getNavioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Navio não encontrado."));
        validarJanela(dto.getAtracacaoPrevista(), dto.getDesatracacaoPrevista());

        VisitaNavio visita = new VisitaNavio();
        visita.setNavio(navio);
        visita.setNumeroViagem(sanitizadorEntrada.limparTextoObrigatorio(dto.getNumeroViagem(), "número da viagem"));
        visita.setAtracacaoPrevista(dto.getAtracacaoPrevista());
        visita.setDesatracacaoPrevista(dto.getDesatracacaoPrevista());
        visita.setObservacoes(tratarTextoOpcional(dto.getObservacoes()));
        visita.setStatus(StatusVisitaNavio.PLANEJADA);
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO atualizar(Long identificador, AtualizacaoVisitaNavioDTO dto) {
        VisitaNavio visita = obter(identificador);
        if (StringUtils.hasText(dto.getNumeroViagem())) {
            visita.setNumeroViagem(sanitizadorEntrada.limparTextoObrigatorio(dto.getNumeroViagem(), "número da viagem"));
        }
        if (dto.getAtracacaoPrevista() != null) {
            visita.setAtracacaoPrevista(dto.getAtracacaoPrevista());
        }
        if (dto.getDesatracacaoPrevista() != null) {
            visita.setDesatracacaoPrevista(dto.getDesatracacaoPrevista());
        }
        if (dto.getObservacoes() != null) {
            visita.setObservacoes(tratarTextoOpcional(dto.getObservacoes()));
        }
        validarJanela(visita.getAtracacaoPrevista(), visita.getDesatracacaoPrevista());
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO planejarAtracacao(Long identificador, PlanejamentoAtracacaoDTO dto) {
        VisitaNavio visita = obter(identificador);
        if (visita.getStatus() == StatusVisitaNavio.CANCELADA || visita.getStatus() == StatusVisitaNavio.DESATRACADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível planejar a atracação de uma visita cancelada ou já desatracada.");
        }
        Berco berco = bercoServico.obter(dto.getBercoId());
        if (berco.getStatus() == StatusBerco.INATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O berço selecionado está inativo.");
        }
        validarJanela(dto.getAtracacaoPrevista(), dto.getDesatracacaoPrevista());
        validarDisponibilidadeBerco(berco.getIdentificador(), identificador,
                dto.getAtracacaoPrevista(), dto.getDesatracacaoPrevista());

        visita.setBerco(berco);
        visita.setAtracacaoPrevista(dto.getAtracacaoPrevista());
        visita.setDesatracacaoPrevista(dto.getDesatracacaoPrevista());
        visita.setStatus(StatusVisitaNavio.PROGRAMADA);
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO registrarAtracacao(Long identificador) {
        VisitaNavio visita = obter(identificador);
        if (visita.getStatus() != StatusVisitaNavio.PROGRAMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A visita precisa estar programada (com berço planejado) para registrar a atracação.");
        }
        visita.setStatus(StatusVisitaNavio.ATRACADA);
        visita.setAtracacaoEfetiva(LocalDateTime.now());
        if (visita.getBerco() != null) {
            visita.getBerco().setStatus(StatusBerco.OCUPADO);
        }
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO iniciarOperacao(Long identificador) {
        VisitaNavio visita = obter(identificador);
        if (visita.getStatus() != StatusVisitaNavio.ATRACADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A visita precisa estar atracada para iniciar a operação.");
        }
        visita.setStatus(StatusVisitaNavio.EM_OPERACAO);
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO concluirOperacao(Long identificador) {
        VisitaNavio visita = obter(identificador);
        if (visita.getStatus() != StatusVisitaNavio.EM_OPERACAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A visita precisa estar em operação para concluí-la.");
        }
        visita.setStatus(StatusVisitaNavio.OPERACAO_CONCLUIDA);
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO registrarDesatracacao(Long identificador) {
        VisitaNavio visita = obter(identificador);
        Set<StatusVisitaNavio> permitidos = EnumSet.of(StatusVisitaNavio.ATRACADA,
                StatusVisitaNavio.EM_OPERACAO, StatusVisitaNavio.OPERACAO_CONCLUIDA);
        if (!permitidos.contains(visita.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só é possível desatracar uma visita atracada.");
        }
        visita.setStatus(StatusVisitaNavio.DESATRACADA);
        visita.setDesatracacaoEfetiva(LocalDateTime.now());
        liberarBerco(visita);
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional
    public VisitaNavioDetalheDTO cancelar(Long identificador) {
        VisitaNavio visita = obter(identificador);
        if (visita.getStatus() == StatusVisitaNavio.DESATRACADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Uma visita já desatracada não pode ser cancelada.");
        }
        visita.setStatus(StatusVisitaNavio.CANCELADA);
        liberarBerco(visita);
        return mapearDetalhe(visitaRepositorio.save(visita));
    }

    @Transactional(readOnly = true)
    public List<OperacaoNavioConteinerDTO> listarOperacoes(Long identificador) {
        return obter(identificador).getOperacoes().stream()
                .map(this::mapearOperacao)
                .collect(Collectors.toList());
    }

    @Transactional
    public OperacaoNavioConteinerDTO adicionarOperacao(Long identificador, OperacaoNavioConteinerRequest request) {
        VisitaNavio visita = obter(identificador);
        OperacaoNavioConteiner operacao = new OperacaoNavioConteiner();
        operacao.setTipoOperacao(request.getTipoOperacao());
        operacao.setIdentificacaoConteiner(sanitizadorEntrada
                .limparTextoObrigatorio(request.getIdentificacaoConteiner(), "identificação do contêiner")
                .toUpperCase(Locale.ROOT));
        operacao.setBay(request.getBay());
        operacao.setFileira(request.getFileira());
        operacao.setAltura(request.getAltura());
        operacao.setPesoToneladas(request.getPesoToneladas());
        operacao.setStatus(StatusOperacaoNavioConteiner.PLANEJADA);
        visita.adicionarOperacao(operacao);
        visitaRepositorio.save(visita);
        return mapearOperacao(operacao);
    }

    @Transactional
    public OperacaoNavioConteinerDTO atualizarStatusOperacao(Long identificador, Long operacaoId,
                                                             AtualizacaoStatusOperacaoDTO dto) {
        VisitaNavio visita = obter(identificador);
        OperacaoNavioConteiner operacao = visita.getOperacoes().stream()
                .filter(item -> item.getIdentificador().equals(operacaoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operação não encontrada para a visita."));
        operacao.setStatus(dto.getStatus());
        operacaoRepositorio.save(operacao);
        return mapearOperacao(operacao);
    }

    @Transactional
    public void removerOperacao(Long identificador, Long operacaoId) {
        VisitaNavio visita = obter(identificador);
        OperacaoNavioConteiner operacao = visita.getOperacoes().stream()
                .filter(item -> item.getIdentificador().equals(operacaoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operação não encontrada para a visita."));
        visita.removerOperacao(operacao);
        visitaRepositorio.save(visita);
    }

    VisitaNavio obter(Long identificador) {
        return visitaRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de navio não encontrada."));
    }

    private void validarJanela(LocalDateTime atracacao, LocalDateTime desatracacao) {
        if (atracacao == null || desatracacao == null) {
            throw new IllegalArgumentException("As datas de atracação e desatracação são obrigatórias.");
        }
        if (!desatracacao.isAfter(atracacao)) {
            throw new IllegalArgumentException("A desatracação prevista deve ser posterior à atracação prevista.");
        }
    }

    private void validarDisponibilidadeBerco(Long bercoId, Long visitaId, LocalDateTime inicio, LocalDateTime fim) {
        Long visitaReferencia = visitaId != null ? visitaId : -1L;
        List<VisitaNavio> conflitos = visitaRepositorio.buscarConflitosDeAtracacao(
                bercoId, visitaReferencia, STATUS_IGNORADOS_CONFLITO, inicio, fim);
        if (!conflitos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma atracação planejada para o berço nessa janela de horário.");
        }
    }

    private void liberarBerco(VisitaNavio visita) {
        if (visita.getBerco() != null) {
            visita.getBerco().setStatus(StatusBerco.DISPONIVEL);
        }
    }

    private String tratarTextoOpcional(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        return StringUtils.hasText(limpo) ? limpo : null;
    }

    private VisitaNavioResumoDTO mapearResumo(VisitaNavio visita) {
        return new VisitaNavioResumoDTO(
                visita.getIdentificador(),
                visita.getNavio() != null ? visita.getNavio().getNome() : null,
                visita.getNavio() != null ? visita.getNavio().getCodigoImo() : null,
                visita.getNumeroViagem(),
                visita.getBerco() != null ? visita.getBerco().getNome() : null,
                visita.getAtracacaoPrevista(),
                visita.getDesatracacaoPrevista(),
                visita.getStatus()
        );
    }

    private VisitaNavioDetalheDTO mapearDetalhe(VisitaNavio visita) {
        List<OperacaoNavioConteinerDTO> operacoes = visita.getOperacoes().stream()
                .map(this::mapearOperacao)
                .collect(Collectors.toList());
        return new VisitaNavioDetalheDTO(
                visita.getIdentificador(),
                visita.getNavio() != null ? visita.getNavio().getIdentificador() : null,
                visita.getNavio() != null ? visita.getNavio().getNome() : null,
                visita.getNavio() != null ? visita.getNavio().getCodigoImo() : null,
                visita.getNumeroViagem(),
                visita.getBerco() != null ? visita.getBerco().getIdentificador() : null,
                visita.getBerco() != null ? visita.getBerco().getNome() : null,
                visita.getAtracacaoPrevista(),
                visita.getAtracacaoEfetiva(),
                visita.getDesatracacaoPrevista(),
                visita.getDesatracacaoEfetiva(),
                visita.getStatus(),
                visita.getObservacoes(),
                operacoes
        );
    }

    private OperacaoNavioConteinerDTO mapearOperacao(OperacaoNavioConteiner operacao) {
        return new OperacaoNavioConteinerDTO(
                operacao.getIdentificador(),
                operacao.getTipoOperacao(),
                operacao.getIdentificacaoConteiner(),
                operacao.getBay(),
                operacao.getFileira(),
                operacao.getAltura(),
                operacao.getPesoToneladas(),
                operacao.getStatus()
        );
    }
}
