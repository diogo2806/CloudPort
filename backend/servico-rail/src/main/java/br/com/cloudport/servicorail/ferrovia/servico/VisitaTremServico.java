package br.com.cloudport.servicorail.ferrovia.servico;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.dto.OperacaoConteinerVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VagaoVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.servico.OrdemMovimentacaoServico;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final SanitizadorEntrada sanitizadorEntrada;
    private final OrdemMovimentacaoServico ordemMovimentacaoServico;

    public VisitaTremServico(VisitaTremRepositorio visitaTremRepositorio,
                              SanitizadorEntrada sanitizadorEntrada,
                              OrdemMovimentacaoServico ordemMovimentacaoServico) {
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
        this.ordemMovimentacaoServico = ordemMovimentacaoServico;
    }

    @Transactional
    public VisitaTremRespostaDto registrarVisita(VisitaTremRequisicaoDto dto) {
        DadosVisitaSanitizados dados = sanitizarDadosBasicos(dto);
        VisitaTrem visita = new VisitaTrem();
        boolean statusAlteradoParaChegou = aplicarDados(visita, dados, dto, true);
        VisitaTrem salvo = visitaTremRepositorio.save(visita);
        if (deveGerarOrdens(statusAlteradoParaChegou, true, salvo)) {
            ordemMovimentacaoServico.gerarOrdensPendentesParaVisita(salvo);
        }
        return VisitaTremRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarVisita(Long id, VisitaTremRequisicaoDto dto) {
        VisitaTrem existente = buscarVisitaComListas(id);
        DadosVisitaSanitizados dados = sanitizarDadosBasicos(dto);
        boolean statusAlteradoParaChegou = aplicarDados(existente, dados, dto, true);
        VisitaTrem atualizado = visitaTremRepositorio.save(existente);
        if (deveGerarOrdens(statusAlteradoParaChegou, true, atualizado)) {
            ordemMovimentacaoServico.gerarOrdensPendentesParaVisita(atualizado);
        }
        return VisitaTremRespostaDto.deEntidade(atualizado);
    }

    @Transactional
    public VisitaTremRespostaDto salvarOuAtualizarPorIdentificador(VisitaTremRequisicaoDto dto,
                                                                   boolean substituirListas) {
        DadosVisitaSanitizados dados = sanitizarDadosBasicos(dto);
        Optional<VisitaTrem> existente = visitaTremRepositorio
                .findByIdentificadorTremIgnoreCase(dados.identificadorTrem);
        VisitaTrem visita = existente
                .map(valor -> buscarVisitaComListas(valor.getId()))
                .orElseGet(VisitaTrem::new);
        boolean statusAlteradoParaChegou = aplicarDados(visita, dados, dto, substituirListas);
        VisitaTrem salvo = visitaTremRepositorio.save(visita);
        if (deveGerarOrdens(statusAlteradoParaChegou, substituirListas, salvo)) {
            ordemMovimentacaoServico.gerarOrdensPendentesParaVisita(salvo);
        }
        return VisitaTremRespostaDto.deEntidade(salvo);
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
    public VisitaTremRespostaDto removerConteinerDescarga(Long idVisita, String codigoConteiner) {
        return removerOperacaoConteiner(idVisita, codigoConteiner, TipoListaOperacaoVisita.DESCARGA);
    }

    @Transactional
    public VisitaTremRespostaDto removerConteinerCarga(Long idVisita, String codigoConteiner) {
        return removerOperacaoConteiner(idVisita, codigoConteiner, TipoListaOperacaoVisita.CARGA);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarStatusDescarga(Long idVisita,
                                                         String codigoConteiner,
                                                         StatusOperacaoConteinerVisita status) {
        return atualizarStatusOperacaoConteiner(idVisita, codigoConteiner, status, TipoListaOperacaoVisita.DESCARGA);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarStatusCarga(Long idVisita,
                                                      String codigoConteiner,
                                                      StatusOperacaoConteinerVisita status) {
        return atualizarStatusOperacaoConteiner(idVisita, codigoConteiner, status, TipoListaOperacaoVisita.CARGA);
    }

    private DadosVisitaSanitizados sanitizarDadosBasicos(VisitaTremRequisicaoDto dto) {
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

        return new DadosVisitaSanitizados(identificadorLimpo, operadoraLimpa, horaChegada, horaPartida, status);
    }

    private boolean aplicarDados(VisitaTrem visita,
                                 DadosVisitaSanitizados dados,
                                 VisitaTremRequisicaoDto dto,
                                 boolean substituirListas) {
        StatusVisitaTrem statusAnterior = visita.getStatusVisita();
        visita.setIdentificadorTrem(dados.identificadorTrem);
        visita.setOperadoraFerroviaria(dados.operadoraFerroviaria);
        visita.setHoraChegadaPrevista(dados.horaChegadaPrevista);
        visita.setHoraPartidaPrevista(dados.horaPartidaPrevista);
        visita.setStatusVisita(dados.statusVisita);

        if (substituirListas) {
            List<VagaoVisita> listaVagoes = converterListaVagoes(dto.getListaVagoes());
            Set<String> identificadoresVagoes = listaVagoes.stream()
                    .map(VagaoVisita::getIdentificadorVagao)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            List<OperacaoConteinerVisita> listaDescarga = converterListaOperacoes(dto.getListaDescarga(), identificadoresVagoes);
            List<OperacaoConteinerVisita> listaCarga = converterListaOperacoes(dto.getListaCarga(), identificadoresVagoes);
            validarListasOperacoes(listaDescarga, listaCarga);
            visita.definirListaDescarga(listaDescarga);
            visita.definirListaCarga(listaCarga);
            visita.definirListaVagoes(listaVagoes);
        }
        return statusAnterior != StatusVisitaTrem.CHEGOU && dados.statusVisita == StatusVisitaTrem.CHEGOU;
    }

    private boolean deveGerarOrdens(boolean statusAlteradoParaChegou,
                                    boolean substituirListas,
                                    VisitaTrem visita) {
        return visita != null
                && visita.getStatusVisita() == StatusVisitaTrem.CHEGOU
                && (statusAlteradoParaChegou || substituirListas);
    }

    private String sanitizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s contém caracteres inválidos.", campo));
        }
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

    private String sanitizarOpcional(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s contém caracteres inválidos.", campo));
        }
        if (!StringUtils.hasText(limpo)) {
            return null;
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

    private List<OperacaoConteinerVisita> converterListaOperacoes(List<OperacaoConteinerVisitaRequisicaoDto> dtos,
                                                                  Set<String> identificadoresVagoes) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .filter(Objects::nonNull)
                .map(dto -> converterParaOperacao(dto, identificadoresVagoes))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void validarListasOperacoes(List<OperacaoConteinerVisita> listaDescarga,
                                        List<OperacaoConteinerVisita> listaCarga) {
        Set<String> codigosDescarga = new HashSet<>();
        for (OperacaoConteinerVisita item : listaDescarga) {
            if (!codigosDescarga.add(item.getCodigoConteiner())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT,
                                "O contêiner %s está duplicado na lista de descarga.", item.getCodigoConteiner()));
            }
        }

        Set<String> codigosCarga = new HashSet<>();
        for (OperacaoConteinerVisita item : listaCarga) {
            if (!codigosCarga.add(item.getCodigoConteiner())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT,
                                "O contêiner %s está duplicado na lista de carga.", item.getCodigoConteiner()));
            }
        }

        Set<String> codigosEmAmbasListas = new HashSet<>(codigosDescarga);
        codigosEmAmbasListas.retainAll(codigosCarga);
        if (!codigosEmAmbasListas.isEmpty()) {
            String codigoConflitante = codigosEmAmbasListas.iterator().next();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format(Locale.ROOT,
                            "O contêiner %s não pode estar nas listas de carga e descarga simultaneamente.", codigoConflitante));
        }
    }

    private List<VagaoVisita> converterListaVagoes(List<VagaoVisitaRequisicaoDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }
        List<VagaoVisita> vagoes = new ArrayList<>();
        Set<Integer> posicoesUtilizadas = new HashSet<>();
        Set<String> identificadoresUtilizados = new HashSet<>();
        for (VagaoVisitaRequisicaoDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            Integer posicao = Optional.ofNullable(dto.getPosicaoNoTrem())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A posição do vagão deve ser informada."));
            if (posicao <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A posição do vagão deve ser um número positivo.");
            }
            if (!posicoesUtilizadas.add(posicao)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Já existe um vagão cadastrado para a posição informada.");
            }
            String identificador = sanitizarObrigatorio(dto.getIdentificadorVagao(),
                    "identificador do vagão", 35).toUpperCase(Locale.ROOT);
            if (!identificadoresUtilizados.add(identificador)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O identificador de vagão informado está duplicado.");
            }
            String tipo = sanitizarOpcional(dto.getTipoVagao(), "tipo do vagão", 40);
            vagoes.add(new VagaoVisita(posicao, identificador, tipo));
        }
        vagoes.sort((a, b) -> Integer.compare(a.getPosicaoNoTrem(), b.getPosicaoNoTrem()));
        return vagoes;
    }

    private VisitaTremRespostaDto adicionarOperacaoConteiner(Long idVisita,
                                                             OperacaoConteinerVisitaRequisicaoDto dto,
                                                             TipoListaOperacaoVisita tipoLista) {
        VisitaTrem visita = buscarVisitaComListas(idVisita);
        Set<String> identificadoresVagoes = obterIdentificadoresVagoes(visita);
        OperacaoConteinerVisita novaOperacao = converterParaOperacao(dto, identificadoresVagoes);

        List<OperacaoConteinerVisita> listaAlvo = obterListaPorTipo(visita, tipoLista);
        boolean jaExiste = listaAlvo.stream()
                .anyMatch(item -> item.getCodigoConteiner().equals(novaOperacao.getCodigoConteiner()));
        if (jaExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O contêiner informado já está planejado para esta visita.");
        }

        List<OperacaoConteinerVisita> listaOposta = obterListaPorTipo(visita, tipoLista.inverso());
        boolean emListaOposta = listaOposta.stream()
                .anyMatch(item -> item.getCodigoConteiner().equals(novaOperacao.getCodigoConteiner()));
        if (emListaOposta) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O contêiner informado já está associado à outra lista desta visita.");
        }

        listaAlvo.add(novaOperacao);
        VisitaTrem atualizado = visitaTremRepositorio.save(visita);
        ordemMovimentacaoServico.registrarOrdemParaOperacaoSeNecessario(atualizado,
                novaOperacao.getCodigoConteiner(),
                converterTipoMovimentacao(tipoLista));
        return VisitaTremRespostaDto.deEntidade(atualizado);
    }

    private VisitaTremRespostaDto removerOperacaoConteiner(Long idVisita,
                                                           String codigoConteiner,
                                                           TipoListaOperacaoVisita tipoLista) {
        VisitaTrem visita = buscarVisitaComListas(idVisita);
        String codigoValidado = sanitizarCodigoConteiner(codigoConteiner);
        List<OperacaoConteinerVisita> listaAlvo = obterListaPorTipo(visita, tipoLista);
        boolean removido = listaAlvo.removeIf(item -> codigoValidado.equals(item.getCodigoConteiner()));
        if (!removido) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "O contêiner informado não está relacionado a esta visita.");
        }
        VisitaTrem atualizado = visitaTremRepositorio.save(visita);
        ordemMovimentacaoServico.removerOrdemSeExistir(atualizado.getId(),
                codigoValidado,
                converterTipoMovimentacao(tipoLista));
        return VisitaTremRespostaDto.deEntidade(atualizado);
    }

    private VisitaTremRespostaDto atualizarStatusOperacaoConteiner(Long idVisita,
                                                                   String codigoConteiner,
                                                                   StatusOperacaoConteinerVisita status,
                                                                   TipoListaOperacaoVisita tipoLista) {
        VisitaTrem visita = buscarVisitaComListas(idVisita);
        String codigoValidado = sanitizarCodigoConteiner(codigoConteiner);
        StatusOperacaoConteinerVisita statusValidado = Optional.ofNullable(status)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O status da operação deve ser informado."));
        List<OperacaoConteinerVisita> listaAlvo = obterListaPorTipo(visita, tipoLista);
        OperacaoConteinerVisita operacao = listaAlvo.stream()
                .filter(item -> codigoValidado.equals(item.getCodigoConteiner()))
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

    private TipoMovimentacaoOrdem converterTipoMovimentacao(TipoListaOperacaoVisita tipoLista) {
        return tipoLista == TipoListaOperacaoVisita.DESCARGA
                ? TipoMovimentacaoOrdem.DESCARGA_TREM
                : TipoMovimentacaoOrdem.CARGA_TREM;
    }

    private OperacaoConteinerVisita converterParaOperacao(OperacaoConteinerVisitaRequisicaoDto dto,
                                                         Set<String> identificadoresVagoes) {
        String codigo = sanitizarCodigoConteiner(dto.getCodigoConteiner());
        StatusOperacaoConteinerVisita status = Optional.ofNullable(dto.getStatusOperacao())
                .orElse(StatusOperacaoConteinerVisita.PENDENTE);
        String identificadorVagao = sanitizarIdentificadorVagao(dto.getIdentificadorVagao(), identificadoresVagoes);
        return new OperacaoConteinerVisita(codigo, status, identificadorVagao);
    }

    private Set<String> obterIdentificadoresVagoes(VisitaTrem visita) {
        return visita.getListaVagoes()
                .stream()
                .map(VagaoVisita::getIdentificadorVagao)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String sanitizarIdentificadorVagao(String identificadorVagao,
                                               Set<String> identificadoresVagoes) {
        String limpo = sanitizadorEntrada.limparTexto(identificadorVagao);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O identificador do vagão contém caracteres inválidos.");
        }
        if (!StringUtils.hasText(limpo)) {
            if (identificadoresVagoes == null || identificadoresVagoes.isEmpty()) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "É necessário informar o vagão associado ao contêiner.");
        }
        String normalizado = limpo.trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() > 35) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O identificador do vagão deve ter no máximo 35 caracteres.");
        }
        if (identificadoresVagoes == null || identificadoresVagoes.isEmpty()) {
            return normalizado;
        }
        if (!identificadoresVagoes.contains(normalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O vagão informado para o contêiner não está cadastrado na visita do trem.");
        }
        return normalizado;
    }

    private String sanitizarCodigoConteiner(String codigoConteiner) {
        String limpo = sanitizadorEntrada.limparTexto(codigoConteiner);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O código do contêiner contém caracteres inválidos.");
        }
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O código do contêiner deve ser informado.");
        }
        String normalizado = limpo.trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O código do contêiner deve ter no máximo 20 caracteres.");
        }
        return normalizado;
    }

    private enum TipoListaOperacaoVisita {
        DESCARGA,
        CARGA;

        TipoListaOperacaoVisita inverso() {
            return this == DESCARGA ? CARGA : DESCARGA;
        }
    }

    private static final class DadosVisitaSanitizados {
        private final String identificadorTrem;
        private final String operadoraFerroviaria;
        private final LocalDateTime horaChegadaPrevista;
        private final LocalDateTime horaPartidaPrevista;
        private final StatusVisitaTrem statusVisita;

        private DadosVisitaSanitizados(String identificadorTrem,
                                       String operadoraFerroviaria,
                                       LocalDateTime horaChegadaPrevista,
                                       LocalDateTime horaPartidaPrevista,
                                       StatusVisitaTrem statusVisita) {
            this.identificadorTrem = identificadorTrem;
            this.operadoraFerroviaria = operadoraFerroviaria;
            this.horaChegadaPrevista = horaChegadaPrevista;
            this.horaPartidaPrevista = horaPartidaPrevista;
            this.statusVisita = statusVisita;
        }
    }
}
