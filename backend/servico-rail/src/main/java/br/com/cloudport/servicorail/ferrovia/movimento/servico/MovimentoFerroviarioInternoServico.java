package br.com.cloudport.servicorail.ferrovia.movimento.servico;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.CancelarMovimentoFerroviarioInternoDto;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.MovimentoFerroviarioInternoRespostaDto;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.PlanejarMovimentoFerroviarioInternoDto;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.MovimentoFerroviarioInterno;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.ReservaRecursoFerroviario;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.TipoRecursoFerroviario;
import br.com.cloudport.servicorail.ferrovia.movimento.repositorio.MovimentoFerroviarioInternoRepositorio;
import br.com.cloudport.servicorail.ferrovia.movimento.repositorio.ReservaRecursoFerroviarioRepositorio;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MovimentoFerroviarioInternoServico {

    private final MovimentoFerroviarioInternoRepositorio movimentoRepositorio;
    private final ReservaRecursoFerroviarioRepositorio reservaRepositorio;
    private final VisitaTremRepositorio visitaTremRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public MovimentoFerroviarioInternoServico(
            MovimentoFerroviarioInternoRepositorio movimentoRepositorio,
            ReservaRecursoFerroviarioRepositorio reservaRepositorio,
            VisitaTremRepositorio visitaTremRepositorio,
            SanitizadorEntrada sanitizadorEntrada) {
        this.movimentoRepositorio = movimentoRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional
    public MovimentoFerroviarioInternoRespostaDto planejar(
            PlanejarMovimentoFerroviarioInternoDto dto,
            String usuario) {
        VisitaTrem visita = visitaTremRepositorio.findById(dto.getVisitaTremId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Visita de trem não encontrada."));
        validarVisitaAtiva(visita);

        String origem = sanitizarObrigatorio(dto.getOrigem(), "origem", 120);
        String destino = sanitizarObrigatorio(dto.getDestino(), "destino", 120);
        if (origem.equalsIgnoreCase(destino)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A origem e o destino do movimento devem ser diferentes.");
        }
        if (StringUtils.hasText(visita.getPosicaoFerroviariaAtual())
                && !visita.getPosicaoFerroviariaAtual().equalsIgnoreCase(origem)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A origem informada não corresponde à posição ferroviária atual da visita.");
        }

        LocalDateTime inicio = validarData(dto.getInicioPlanejado(), "início planejado");
        LocalDateTime fim = validarData(dto.getFimPlanejado(), "fim planejado");
        if (!fim.isAfter(inicio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O fim planejado deve ser posterior ao início planejado.");
        }

        String usuarioNormalizado = sanitizarObrigatorio(usuario, "usuário", 120);
        MovimentoFerroviarioInterno movimento = new MovimentoFerroviarioInterno(
                visita,
                origem,
                destino,
                inicio,
                fim,
                usuarioNormalizado);

        adicionarRecursos(movimento, TipoRecursoFerroviario.ROTA, dto.getRotas());
        adicionarRecursos(movimento, TipoRecursoFerroviario.LINHA, dto.getLinhas());
        adicionarRecursos(movimento, TipoRecursoFerroviario.TRECHO, dto.getTrechos());
        adicionarRecursos(movimento, TipoRecursoFerroviario.SWITCH, dto.getSwitches());

        if (movimento.getRecursos().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe ao menos uma rota, linha, trecho ou switch.");
        }

        MovimentoFerroviarioInterno salvo = movimentoRepositorio.save(movimento);
        return MovimentoFerroviarioInternoRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public MovimentoFerroviarioInternoRespostaDto autorizar(Long id, String usuario) {
        MovimentoFerroviarioInterno movimento = buscarComBloqueio(id);
        validarVisitaAtiva(movimento.getVisitaTrem());
        validarConflitos(movimento);
        executarTransicao(() -> movimento.autorizar(sanitizarObrigatorio(usuario, "usuário", 120)));
        return salvarComDeteccaoDeConflito(movimento);
    }

    @Transactional
    public MovimentoFerroviarioInternoRespostaDto iniciar(Long id, String usuario) {
        MovimentoFerroviarioInterno movimento = buscarComBloqueio(id);
        executarTransicao(() -> movimento.iniciar(sanitizarObrigatorio(usuario, "usuário", 120)));
        MovimentoFerroviarioInterno salvo = movimentoRepositorio.save(movimento);
        return MovimentoFerroviarioInternoRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public MovimentoFerroviarioInternoRespostaDto concluir(Long id, String usuario) {
        MovimentoFerroviarioInterno movimento = buscarComBloqueio(id);
        executarTransicao(() -> movimento.concluir(sanitizarObrigatorio(usuario, "usuário", 120)));
        movimento.getVisitaTrem().setPosicaoFerroviariaAtual(movimento.getDestino());
        visitaTremRepositorio.save(movimento.getVisitaTrem());
        MovimentoFerroviarioInterno salvo = movimentoRepositorio.save(movimento);
        return MovimentoFerroviarioInternoRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public MovimentoFerroviarioInternoRespostaDto cancelar(
            Long id,
            CancelarMovimentoFerroviarioInternoDto dto,
            String usuario) {
        MovimentoFerroviarioInterno movimento = buscarComBloqueio(id);
        String motivo = sanitizarObrigatorio(dto.getMotivo(), "motivo do cancelamento", 500);
        executarTransicao(() -> movimento.cancelar(
                motivo,
                sanitizarObrigatorio(usuario, "usuário", 120)));
        MovimentoFerroviarioInterno salvo = movimentoRepositorio.save(movimento);
        return MovimentoFerroviarioInternoRespostaDto.deEntidade(salvo);
    }

    @Transactional(readOnly = true)
    public MovimentoFerroviarioInternoRespostaDto consultar(Long id) {
        MovimentoFerroviarioInterno movimento = movimentoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movimento ferroviário interno não encontrado."));
        return MovimentoFerroviarioInternoRespostaDto.deEntidade(movimento);
    }

    @Transactional(readOnly = true)
    public List<MovimentoFerroviarioInternoRespostaDto> listarPorVisita(Long visitaTremId) {
        if (!visitaTremRepositorio.existsById(visitaTremId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de trem não encontrada.");
        }
        return movimentoRepositorio.findByVisitaTrem_IdOrderByCriadoEmDesc(visitaTremId)
                .stream()
                .map(MovimentoFerroviarioInternoRespostaDto::deEntidade)
                .collect(Collectors.toList());
    }

    private void executarTransicao(Runnable transicao) {
        try {
            transicao.run();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private MovimentoFerroviarioInterno buscarComBloqueio(Long id) {
        return movimentoRepositorio.findOneById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movimento ferroviário interno não encontrado."));
    }

    private void validarVisitaAtiva(VisitaTrem visita) {
        if (visita.getStatusVisita() == StatusVisitaTrem.PARTIU) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é permitido movimentar uma visita de trem que já partiu.");
        }
    }

    private void validarConflitos(MovimentoFerroviarioInterno movimento) {
        movimentoRepositorio
                .findFirstByVisitaTrem_IdAndReservaAtivaTrueAndInicioPlanejadoLessThanAndFimPlanejadoGreaterThanOrderByInicioPlanejadoAsc(
                        movimento.getVisitaTrem().getId(),
                        movimento.getFimPlanejado(),
                        movimento.getInicioPlanejado())
                .ifPresent(conflito -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A visita já possui outro movimento autorizado para a janela informada.");
                });

        for (ReservaRecursoFerroviario recurso : movimento.getRecursos()) {
            reservaRepositorio
                    .findFirstByTipoRecursoAndCodigoRecursoIgnoreCaseAndAtivoTrueAndInicioReservaLessThanAndFimReservaGreaterThanOrderByInicioReservaAsc(
                            recurso.getTipoRecurso(),
                            recurso.getCodigoRecurso(),
                            movimento.getFimPlanejado(),
                            movimento.getInicioPlanejado())
                    .ifPresent(conflito -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                String.format(
                                        Locale.ROOT,
                                        "O recurso ferroviário %s %s já está reservado no período.",
                                        recurso.getTipoRecurso(),
                                        recurso.getCodigoRecurso()));
                    });
        }
    }

    private MovimentoFerroviarioInternoRespostaDto salvarComDeteccaoDeConflito(
            MovimentoFerroviarioInterno movimento) {
        try {
            MovimentoFerroviarioInterno salvo = movimentoRepositorio.saveAndFlush(movimento);
            return MovimentoFerroviarioInternoRespostaDto.deEntidade(salvo);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    mensagemConflito(ex));
        }
    }

    private String mensagemConflito(DataIntegrityViolationException ex) {
        String mensagem = cadeiaMensagens(ex).toLowerCase(Locale.ROOT);
        if (mensagem.contains("ex_movimento_visita_periodo")) {
            return "A visita já possui outro movimento autorizado para a janela informada.";
        }
        return "Uma rota, linha, trecho ou switch já está reservado no período informado.";
    }

    private String cadeiaMensagens(Throwable erro) {
        StringBuilder mensagens = new StringBuilder();
        Throwable atual = erro;
        while (atual != null) {
            if (atual.getMessage() != null) {
                mensagens.append(' ').append(atual.getMessage());
            }
            atual = atual.getCause();
        }
        return mensagens.toString();
    }

    private void adicionarRecursos(MovimentoFerroviarioInterno movimento,
                                    TipoRecursoFerroviario tipo,
                                    List<String> codigos) {
        Set<String> normalizados = normalizarRecursos(codigos, tipo);
        normalizados.forEach(codigo -> movimento.adicionarRecurso(tipo, codigo));
    }

    private Set<String> normalizarRecursos(List<String> codigos, TipoRecursoFerroviario tipo) {
        if (codigos == null) {
            return Collections.emptySet();
        }
        Set<String> normalizados = new LinkedHashSet<>();
        for (String codigo : new ArrayList<>(codigos)) {
            normalizados.add(
                    sanitizarObrigatorio(
                            codigo,
                            "código de " + tipo.name().toLowerCase(Locale.ROOT),
                            80).toUpperCase(Locale.ROOT));
        }
        return normalizados;
    }

    private LocalDateTime validarData(LocalDateTime valor, String campo) {
        if (valor == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O campo " + campo + " deve ser informado.");
        }
        return valor.truncatedTo(ChronoUnit.MINUTES);
    }

    private String sanitizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O campo " + campo + " contém caracteres inválidos.");
        }
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O campo " + campo + " é obrigatório.");
        }
        String normalizado = limpo.trim();
        if (normalizado.length() > tamanhoMaximo) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            Locale.ROOT,
                            "O campo %s deve ter no máximo %d caracteres.",
                            campo,
                            tamanhoMaximo));
        }
        return normalizado;
    }
}
