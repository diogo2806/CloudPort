package br.com.cloudport.servicoyard.ferrovia.servico;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.container.repositorio.ConteinerRepositorio;
import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.ferrovia.dto.OperacaoConteinerVisitaRequisicaoDto;
import br.com.cloudport.servicoyard.ferrovia.dto.VisitaTremRequisicaoDto;
import br.com.cloudport.servicoyard.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicoyard.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicoyard.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicoyard.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicoyard.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicoyard.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitaTremServico {

    private static final int DIAS_MAXIMO_CONSULTA = 30;

    private final VisitaTremRepositorio visitaTremRepositorio;
    private final ConteinerRepositorio conteinerRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public VisitaTremServico(VisitaTremRepositorio visitaTremRepositorio,
                              ConteinerRepositorio conteinerRepositorio,
                              SanitizadorEntrada sanitizadorEntrada) {
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional
    public VisitaTremRespostaDto registrarVisita(VisitaTremRequisicaoDto dto) {
        VisitaTrem visita = new VisitaTrem();
        aplicarDados(visita, dto);
        VisitaTrem salvo = visitaTremRepositorio.save(visita);
        return VisitaTremRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarVisita(Long id, VisitaTremRequisicaoDto dto) {
        VisitaTrem existente = buscarVisitaComListas(id);
        aplicarDados(existente, dto);
        VisitaTrem atualizado = visitaTremRepositorio.save(existente);
        return VisitaTremRespostaDto.deEntidade(atualizado);
    }

    @Transactional(readOnly = true)
    public VisitaTremRespostaDto consultarVisita(Long id) {
        VisitaTrem visita = buscarVisitaComListas(id);
        return VisitaTremRespostaDto.deEntidade(visita);
    }

    @Transactional(readOnly = true)
    public List<VisitaTremRespostaDto> listarVisitasProximosDias(int dias) {
        if (dias < 1 || dias > DIAS_MAXIMO_CONSULTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O intervalo de consulta deve estar entre 1 e %d dias.", DIAS_MAXIMO_CONSULTA));
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.minusDays(1);
        LocalDateTime limite = agora.plusDays(dias);
        return visitaTremRepositorio.buscarVisitasPlanejadasOuAtivas(inicio, agora, limite, StatusVisitaTrem.PARTIU)
                .stream()
                .distinct()
                .map(VisitaTremRespostaDto::deEntidadeSemListas)
                .collect(Collectors.toList());
    }

    @Transactional
    public VisitaTremRespostaDto adicionarConteinerDescarga(Long idVisita, OperacaoConteinerVisitaRequisicaoDto dto) {
        return adicionarOperacaoConteiner(idVisita, dto, TipoListaOperacaoVisita.DESCARGA);
    }

    @Transactional
    public VisitaTremRespostaDto adicionarConteinerCarga(Long idVisita, OperacaoConteinerVisitaRequisicaoDto dto) {
        return adicionarOperacaoConteiner(idVisita, dto, TipoListaOperacaoVisita.CARGA);
    }

    @Transactional
    public VisitaTremRespostaDto removerConteinerDescarga(Long idVisita, Long idConteiner) {
        return removerOperacaoConteiner(idVisita, idConteiner, TipoListaOperacaoVisita.DESCARGA);
    }

    @Transactional
    public VisitaTremRespostaDto removerConteinerCarga(Long idVisita, Long idConteiner) {
        return removerOperacaoConteiner(idVisita, idConteiner, TipoListaOperacaoVisita.CARGA);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarStatusDescarga(Long idVisita,
                                                         Long idConteiner,
                                                         StatusOperacaoConteinerVisita status) {
        return atualizarStatusOperacaoConteiner(idVisita, idConteiner, status, TipoListaOperacaoVisita.DESCARGA);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarStatusCarga(Long idVisita,
                                                      Long idConteiner,
                                                      StatusOperacaoConteinerVisita status) {
        return atualizarStatusOperacaoConteiner(idVisita, idConteiner, status, TipoListaOperacaoVisita.CARGA);
    }

    private void aplicarDados(VisitaTrem visita, VisitaTremRequisicaoDto dto) {
        String identificadorLimpo = sanitizarObrigatorio(dto.getIdentificadorTrem(), "identificador do trem", 40)
                .toUpperCase(Locale.ROOT);
        String operadoraLimpa = sanitizarObrigatorio(dto.getOperadoraFerroviaria(), "operadora ferroviária", 80);

        LocalDateTime horaChegada = Objects.requireNonNull(dto.getHoraChegadaPrevista(),
                "A hora prevista de chegada deve ser informada.");
        LocalDateTime horaPartida = Objects.requireNonNull(dto.getHoraPartidaPrevista(),
                "A hora prevista de partida deve ser informada.");

        if (horaPartida.isBefore(horaChegada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A partida prevista não pode ser anterior à chegada prevista.");
        }

        horaChegada = horaChegada.truncatedTo(ChronoUnit.MINUTES);
        horaPartida = horaPartida.truncatedTo(ChronoUnit.MINUTES);

        StatusVisitaTrem status = Objects.requireNonNull(dto.getStatusVisita(),
                "O status da visita deve ser informado.");

        visita.setIdentificadorTrem(identificadorLimpo);
        visita.setOperadoraFerroviaria(operadoraLimpa);
        visita.setHoraChegadaPrevista(horaChegada);
        visita.setHoraPartidaPrevista(horaPartida);
        visita.setStatusVisita(status);
    }

    private String sanitizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s é obrigatório.", campo));
        }
        String normalizado = limpo.trim();
        if (normalizado.length() > tamanhoMaximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s deve ter no máximo %d caracteres.", campo, tamanhoMaximo));
        }
        return normalizado;
    }

    private VisitaTrem buscarVisitaComListas(Long id) {
        return visitaTremRepositorio.buscarPorIdComListas(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de trem não encontrada."));
    }

    private VisitaTremRespostaDto adicionarOperacaoConteiner(Long idVisita,
                                                             OperacaoConteinerVisitaRequisicaoDto dto,
                                                             TipoListaOperacaoVisita tipoLista) {
        VisitaTrem visita = buscarVisitaComListas(idVisita);
        OperacaoConteinerVisita novaOperacao = converterParaOperacao(dto);

        List<OperacaoConteinerVisita> listaAlvo = obterListaPorTipo(visita, tipoLista);
        boolean jaExiste = listaAlvo.stream()
                .anyMatch(item -> item.getIdConteiner().equals(novaOperacao.getIdConteiner()));
        if (jaExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O contêiner informado já está planejado para esta visita.");
        }

        List<OperacaoConteinerVisita> listaOposta = obterListaPorTipo(visita, tipoLista.inverso());
        boolean emListaOposta = listaOposta.stream()
                .anyMatch(item -> item.getIdConteiner().equals(novaOperacao.getIdConteiner()));
        if (emListaOposta) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O contêiner informado já está associado à outra lista desta visita.");
        }

        listaAlvo.add(novaOperacao);
        visitaTremRepositorio.save(visita);
        return VisitaTremRespostaDto.deEntidade(visita);
    }

    private VisitaTremRespostaDto removerOperacaoConteiner(Long idVisita,
                                                           Long idConteiner,
                                                           TipoListaOperacaoVisita tipoLista) {
        VisitaTrem visita = buscarVisitaComListas(idVisita);
        Long idValidado = validarIdConteiner(idConteiner);
        List<OperacaoConteinerVisita> listaAlvo = obterListaPorTipo(visita, tipoLista);
        boolean removido = listaAlvo.removeIf(item -> idValidado.equals(item.getIdConteiner()));
        if (!removido) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "O contêiner informado não está relacionado a esta visita.");
        }
        visitaTremRepositorio.save(visita);
        return VisitaTremRespostaDto.deEntidade(visita);
    }

    private VisitaTremRespostaDto atualizarStatusOperacaoConteiner(Long idVisita,
                                                                   Long idConteiner,
                                                                   StatusOperacaoConteinerVisita status,
                                                                   TipoListaOperacaoVisita tipoLista) {
        VisitaTrem visita = buscarVisitaComListas(idVisita);
        Long idValidado = validarIdConteiner(idConteiner);
        StatusOperacaoConteinerVisita statusValidado = Optional.ofNullable(status)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O status da operação deve ser informado."));
        List<OperacaoConteinerVisita> listaAlvo = obterListaPorTipo(visita, tipoLista);
        OperacaoConteinerVisita operacao = listaAlvo.stream()
                .filter(item -> idValidado.equals(item.getIdConteiner()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "O contêiner informado não está relacionado a esta visita."));
        operacao.setStatusOperacao(statusValidado);
        visitaTremRepositorio.save(visita);
        return VisitaTremRespostaDto.deEntidade(visita);
    }

    private List<OperacaoConteinerVisita> obterListaPorTipo(VisitaTrem visita, TipoListaOperacaoVisita tipoLista) {
        if (tipoLista == TipoListaOperacaoVisita.DESCARGA) {
            return visita.getListaDescarga();
        }
        return visita.getListaCarga();
    }

    private OperacaoConteinerVisita converterParaOperacao(OperacaoConteinerVisitaRequisicaoDto dto) {
        Long idConteiner = validarIdConteiner(dto.getIdConteiner());
        validarExistenciaConteiner(idConteiner);
        StatusOperacaoConteinerVisita status = Optional.ofNullable(dto.getStatusOperacao())
                .orElse(StatusOperacaoConteinerVisita.PENDENTE);
        return new OperacaoConteinerVisita(idConteiner, status);
    }

    private Long validarIdConteiner(Long idConteiner) {
        if (idConteiner == null || idConteiner <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O identificador do contêiner deve ser um número positivo.");
        }
        return idConteiner;
    }

    private void validarExistenciaConteiner(Long idConteiner) {
        if (!conteinerRepositorio.existsById(idConteiner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "O contêiner informado não foi localizado no pátio.");
        }
    }

    private enum TipoListaOperacaoVisita {
        DESCARGA,
        CARGA;

        TipoListaOperacaoVisita inverso() {
            return this == DESCARGA ? CARGA : DESCARGA;
        }
    }
}
