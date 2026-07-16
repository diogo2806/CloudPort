package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.AplicacaoPlanoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.ItemPlanoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.ResultadoAplicacaoPlanoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.AplicacaoPlanoOtimizadoNavioPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusAplicacaoPlanoOtimizadoNavioPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemPlanoOtimizadoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OtimizacaoGlobalNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.AplicacaoPlanoOtimizadoNavioPatioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AplicacaoPlanoOtimizadoNavioPatioServico {

    private static final int VERSAO_PLANO = 1;

    private final OtimizacaoGlobalNavioPatioServico otimizacaoServico;
    private final PersistenciaPlanoOtimizadoNavioPatioServico persistenciaServico;
    private final PlanoOtimizadoYardCliente planoOtimizadoYardCliente;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final VisitaNavioServico visitaServico;
    private final AplicacaoPlanoOtimizadoNavioPatioRepositorio aplicacaoRepositorio;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final long timeoutAplicacaoMinutos;

    public AplicacaoPlanoOtimizadoNavioPatioServico(
            OtimizacaoGlobalNavioPatioServico otimizacaoServico,
            PersistenciaPlanoOtimizadoNavioPatioServico persistenciaServico,
            PlanoOtimizadoYardCliente planoOtimizadoYardCliente,
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            VisitaNavioServico visitaServico,
            AplicacaoPlanoOtimizadoNavioPatioRepositorio aplicacaoRepositorio,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Value("${cloudport.integracao.yard.plano-aplicacao-timeout-minutos:30}")
            long timeoutAplicacaoMinutos
    ) {
        this.otimizacaoServico = otimizacaoServico;
        this.persistenciaServico = persistenciaServico;
        this.planoOtimizadoYardCliente = planoOtimizadoYardCliente;
        this.itemRepositorio = itemRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.visitaServico = visitaServico;
        this.aplicacaoRepositorio = aplicacaoRepositorio;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.timeoutAplicacaoMinutos = Math.max(1, timeoutAplicacaoMinutos);
    }

    public ResultadoAplicacaoPlano replanejar(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando
    ) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        boolean aplicar = comando != null && comando.aplicarEfetivo();
        String usuario = comando == null || !StringUtils.hasText(comando.usuario())
                ? "sistema"
                : comando.usuario().trim();

        OtimizacaoGlobalNavioPatioDTO otimizacao = otimizacaoServico.otimizar(visitaId);
        PlanoInterpretado plano = interpretar(visitaId, otimizacao, comando);
        if (aplicar && !plano.alertasImpeditivos().isEmpty()) {
            throw new IllegalStateException("O plano otimizado nao pode ser aplicado: "
                    + String.join(" ", plano.alertasImpeditivos()));
        }
        if (!aplicar) {
            return resultadoPlano(plano, reservasSugeridas(plano.itens()));
        }

        ResultadoAplicacaoPlano resultadoAnterior = buscarOuRegistrarAplicacaoPlano(
                visitaId,
                plano.planoId());
        if (resultadoAnterior != null) {
            return resultadoAnterior;
        }

        AplicacaoPlanoYardDTO comandoYard = montarComandoYard(visitaId, usuario, plano);
        ResultadoAplicacaoPlanoYardDTO resultadoYard;
        try {
            resultadoYard = planoOtimizadoYardCliente.aplicar(comandoYard);
        } catch (RuntimeException ex) {
            marcarFalha(visitaId, plano.planoId(), ex);
            throw ex;
        }

        try {
            return concluirAplicacao(visita, usuario, plano);
        } catch (RuntimeException ex) {
            try {
                planoOtimizadoYardCliente.compensar(
                        plano.planoId(),
                        visitaId,
                        usuario,
                        "Falha ao persistir reservas e itens do plano no modulo Navio: " + ex.getMessage(),
                        resultadoYard.getEstadosAnteriores(),
                        resultadoYard.getEstadosAnterioresWorkQueues());
            } catch (RuntimeException compensacaoEx) {
                ex.addSuppressed(compensacaoEx);
            }
            marcarFalha(visitaId, plano.planoId(), ex);
            throw ex;
        }
    }

    private ResultadoAplicacaoPlano buscarOuRegistrarAplicacaoPlano(
            Long visitaId,
            String planoId
    ) {
        try {
            return executarRegistroAplicacao(visitaId, planoId);
        } catch (DataIntegrityViolationException ex) {
            return executarRegistroAplicacao(visitaId, planoId);
        }
    }

    private ResultadoAplicacaoPlano executarRegistroAplicacao(
            Long visitaId,
            String planoId
    ) {
        ResultadoAplicacaoPlano resultado = requiresNewTransactionTemplate.execute(status -> {
            LocalDateTime agora = LocalDateTime.now();
            AplicacaoPlanoOtimizadoNavioPatio aplicacao = aplicacaoRepositorio
                    .findByPlanoIdAndVisitaNavioId(planoId, visitaId)
                    .orElse(null);
            if (aplicacao == null) {
                AplicacaoPlanoOtimizadoNavioPatio novaAplicacao =
                        new AplicacaoPlanoOtimizadoNavioPatio();
                novaAplicacao.setPlanoId(planoId);
                novaAplicacao.setVisitaNavioId(visitaId);
                novaAplicacao.setStatus(StatusAplicacaoPlanoOtimizadoNavioPatio.EM_ANDAMENTO);
                novaAplicacao.setCriadoEm(agora);
                novaAplicacao.setAtualizadoEm(agora);
                aplicacaoRepositorio.saveAndFlush(novaAplicacao);
                return null;
            }
            if (aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoNavioPatio.CONCLUIDA) {
                return desserializarResultado(aplicacao);
            }
            boolean emAndamentoRecente =
                    aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoNavioPatio.EM_ANDAMENTO
                            && aplicacao.getAtualizadoEm() != null
                            && aplicacao.getAtualizadoEm().isAfter(
                                    agora.minusMinutes(timeoutAplicacaoMinutos));
            if (emAndamentoRecente) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "O plano " + planoId + " ja esta em aplicacao.");
            }
            aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoNavioPatio.EM_ANDAMENTO);
            aplicacao.setResultadoJson(null);
            aplicacao.setErro(null);
            aplicacao.setAtualizadoEm(agora);
            aplicacaoRepositorio.save(aplicacao);
            return null;
        });
        return resultado;
    }

    private ResultadoAplicacaoPlano concluirAplicacao(
            VisitaNavio visita,
            String usuario,
            PlanoInterpretado plano
    ) {
        ResultadoAplicacaoPlano resultado = transactionTemplate.execute(status -> {
            AplicacaoPlanoOtimizadoNavioPatio aplicacao = aplicacaoRepositorio
                    .findByPlanoIdAndVisitaNavioId(plano.planoId(), visita.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "O registro idempotente do plano nao foi encontrado."));
            if (aplicacao.getStatus() == StatusAplicacaoPlanoOtimizadoNavioPatio.CONCLUIDA) {
                return desserializarResultado(aplicacao);
            }
            if (aplicacao.getStatus() != StatusAplicacaoPlanoOtimizadoNavioPatio.EM_ANDAMENTO) {
                throw new IllegalStateException("O plano " + plano.planoId()
                        + " nao esta disponivel para conclusao.");
            }

            List<ReservaPatioNavioDTO> reservas =
                    persistenciaServico.aplicar(visita.getId(), plano.planoId(), plano.itens());
            ResultadoAplicacaoPlano resultadoAplicado = resultadoPlano(plano, reservas);
            String resultadoJson = serializarResultado(resultadoAplicado);
            visitaServico.registrarEvento(
                    visita,
                    null,
                    "PLANO_OTIMIZADO_PATIO_APLICADO",
                    "Plano " + plano.planoId() + " versao " + VERSAO_PLANO
                            + " aplicado atomicamente em " + reservas.size()
                            + " item(ns), com ganho calculado de "
                            + plano.economiaPercentual() + "%.",
                    usuario,
                    null,
                    plano.planoId());
            aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoNavioPatio.CONCLUIDA);
            aplicacao.setResultadoJson(resultadoJson);
            aplicacao.setErro(null);
            aplicacao.setAtualizadoEm(LocalDateTime.now());
            aplicacaoRepositorio.save(aplicacao);
            return resultadoAplicado;
        });
        if (resultado == null) {
            throw new IllegalStateException("A conclusao do plano otimizado nao retornou resultado.");
        }
        return resultado;
    }

    private void marcarFalha(Long visitaId, String planoId, RuntimeException falha) {
        requiresNewTransactionTemplate.execute(status -> {
            aplicacaoRepositorio.findByPlanoIdAndVisitaNavioId(planoId, visitaId)
                    .filter(aplicacao ->
                            aplicacao.getStatus() != StatusAplicacaoPlanoOtimizadoNavioPatio.CONCLUIDA)
                    .ifPresent(aplicacao -> {
                        aplicacao.setStatus(StatusAplicacaoPlanoOtimizadoNavioPatio.FALHA);
                        aplicacao.setErro(limitar(
                                falha.getMessage() == null
                                        ? falha.getClass().getSimpleName()
                                        : falha.getMessage(),
                                2000));
                        aplicacao.setAtualizadoEm(LocalDateTime.now());
                        aplicacaoRepositorio.save(aplicacao);
                    });
            return null;
        });
    }

    private ResultadoAplicacaoPlano resultadoPlano(
            PlanoInterpretado plano,
            List<ReservaPatioNavioDTO> reservas
    ) {
        return new ResultadoAplicacaoPlano(
                plano.planoId(),
                VERSAO_PLANO,
                List.copyOf(reservas),
                plano.economiaPercentual(),
                plano.riscoRehandle(),
                plano.alertasImpeditivos(),
                plano.itensNaoReplanejados(),
                plano.distanciaOriginal(),
                plano.distanciaOtimizada());
    }

    private String serializarResultado(ResultadoAplicacaoPlano resultado) {
        try {
            return objectMapper.writeValueAsString(resultado);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel persistir o resultado do plano otimizado.", ex);
        }
    }

    private ResultadoAplicacaoPlano desserializarResultado(
            AplicacaoPlanoOtimizadoNavioPatio aplicacao
    ) {
        if (!StringUtils.hasText(aplicacao.getResultadoJson())) {
            throw new IllegalStateException("A aplicacao concluida do plano nao possui resultado persistido.");
        }
        try {
            return objectMapper.readValue(
                    aplicacao.getResultadoJson(),
                    ResultadoAplicacaoPlano.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel recuperar o resultado do plano otimizado.", ex);
        }
    }

    private String limitar(String valor, int tamanhoMaximo) {
        return valor.length() <= tamanhoMaximo ? valor : valor.substring(0, tamanhoMaximo);
    }

    private PlanoInterpretado interpretar(
            Long visitaId,
            OtimizacaoGlobalNavioPatioDTO otimizacao,
            ComandoReplanejamentoPatioNavioDTO comando
    ) {
        Map<String, ItemOperacaoNavio> itensPorCodigo = new LinkedHashMap<>();
        List<String> alertas = new ArrayList<>();
        List<Long> naoReplanejados = new ArrayList<>();
        for (ItemOperacaoNavio item : itemRepositorio.findByVisitaNavioId(visitaId)) {
            String chave = normalizar(item.getCodigoLote());
            if (itensPorCodigo.putIfAbsent(chave, item) != null) {
                alertas.add("A visita possui codigo de carga duplicado no plano: " + item.getCodigoLote() + ".");
            }
        }
        Map<Long, ReservaPosicaoPatioNavio> reservasAtivas = reservasAtivasPorItem(visitaId);
        Map<String, ItemPlanoOtimizadoNavioPatioDTO> atribuicoes = new LinkedHashMap<>();
        Map<String, Object> planoBruto = otimizacao.planoOtimizado() == null
                ? Map.of()
                : otimizacao.planoOtimizado();
        int sequencia = 0;

        for (Map<String, Object> job : listaMapas(planoBruto.get("jobsDualCycle"))) {
            sequencia = adicionarAtribuicao(
                    atribuicoes,
                    itensPorCodigo,
                    reservasAtivas,
                    texto(job.get("containerPickup")),
                    inteiro(job.get("linhaPickup")),
                    inteiro(job.get("colunaPickup")),
                    texto(job.get("equipamentoId")),
                    sequencia,
                    alertas,
                    naoReplanejados);
            sequencia = adicionarAtribuicao(
                    atribuicoes,
                    itensPorCodigo,
                    reservasAtivas,
                    texto(job.get("containerDropoff")),
                    inteiro(job.get("linhaDropoff")),
                    inteiro(job.get("colunaDropoff")),
                    texto(job.get("equipamentoId")),
                    sequencia,
                    alertas,
                    naoReplanejados);
        }

        for (Map<String, Object> rota : listaMapas(planoBruto.get("rotasEquipamento"))) {
            String equipamento = texto(rota.get("equipamentoId"));
            for (Map<String, Object> parada : listaMapas(rota.get("paradas"))) {
                sequencia = adicionarAtribuicao(
                        atribuicoes,
                        itensPorCodigo,
                        reservasAtivas,
                        texto(parada.get("codigoContainer")),
                        inteiro(parada.get("linha")),
                        inteiro(parada.get("coluna")),
                        equipamento,
                        sequencia,
                        alertas,
                        naoReplanejados);
            }
        }

        int totalEsperado = otimizacao.itensImportacaoConsiderados()
                + otimizacao.itensExportacaoConsiderados();
        if (atribuicoes.size() != totalEsperado) {
            alertas.add("O otimizador retornou " + atribuicoes.size()
                    + " atribuicao(oes) aplicavel(is), mas " + totalEsperado
                    + " item(ns) foram considerados.");
        }
        if (atribuicoes.isEmpty()) {
            alertas.add("O resultado do otimizador nao contem itens aplicaveis.");
        }

        List<ItemPlanoOtimizadoNavioPatioDTO> itensPlano = atribuicoes.values().stream()
                .sorted(Comparator.comparing(ItemPlanoOtimizadoNavioPatioDTO::sequenciaPlano))
                .toList();
        int distanciaOtimizada = distanciaOtimizada(planoBruto, itensPlano);
        int economiaAbsoluta = Math.max(0, inteiroOuZero(planoBruto.get("distanciaEconomizada")));
        int distanciaOriginal = Math.max(distanciaOtimizada, distanciaOtimizada + economiaAbsoluta);
        BigDecimal economiaPercentual = distanciaOriginal == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(economiaAbsoluta)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(distanciaOriginal), 2, RoundingMode.HALF_UP);
        int rehandlesEstimados = Math.max(
                0,
                inteiroOuZero(planoBruto.get("totalOperacoes"))
                        - (inteiroOuZero(planoBruto.get("operacoesDualCycle")) * 2));
        if (comando != null && comando.limiteRehandleAceitavel() != null
                && rehandlesEstimados > comando.limiteRehandleAceitavel()) {
            alertas.add("O plano estima " + rehandlesEstimados
                    + " operacao(oes) fora de dual cycle, acima do limite "
                    + comando.limiteRehandleAceitavel() + ".");
        }
        String risco = calcularRisco(
                alertas,
                decimal(planoBruto.get("eficienciaMedia")),
                texto(planoBruto.get("statusGeral")),
                rehandlesEstimados,
                totalEsperado);
        String planoId = gerarPlanoId(visitaId, itensPlano, distanciaOriginal, distanciaOtimizada);
        return new PlanoInterpretado(
                planoId,
                itensPlano,
                economiaPercentual,
                risco,
                List.copyOf(new LinkedHashSet<>(alertas)),
                List.copyOf(new LinkedHashSet<>(naoReplanejados)),
                distanciaOriginal,
                distanciaOtimizada);
    }

    private int adicionarAtribuicao(
            Map<String, ItemPlanoOtimizadoNavioPatioDTO> atribuicoes,
            Map<String, ItemOperacaoNavio> itensPorCodigo,
            Map<Long, ReservaPosicaoPatioNavio> reservasAtivas,
            String codigo,
            Integer linha,
            Integer coluna,
            String equipamento,
            int sequenciaAtual,
            List<String> alertas,
            List<Long> naoReplanejados
    ) {
        if (!StringUtils.hasText(codigo)) {
            alertas.add("O plano contem uma operacao sem codigo de carga.");
            return sequenciaAtual;
        }
        String chave = normalizar(codigo);
        if (atribuicoes.containsKey(chave)) {
            return sequenciaAtual;
        }
        ItemOperacaoNavio item = itensPorCodigo.get(chave);
        if (item == null) {
            alertas.add("A carga " + codigo + " retornada pelo otimizador nao pertence a visita.");
            return sequenciaAtual;
        }
        if (item.getStatus() == StatusItemCarga.BLOQUEADO
                || item.getStatus() == StatusItemCarga.OPERADO
                || item.getStatus() == StatusItemCarga.CANCELADO) {
            alertas.add("O item " + codigo + " nao pode ser aplicado no status " + item.getStatus() + ".");
            naoReplanejados.add(item.getId());
            return sequenciaAtual;
        }
        ReservaPosicaoPatioNavio reserva = reservasAtivas.get(item.getId());
        if (reserva == null) {
            alertas.add("O item " + codigo + " nao possui reserva real ativa.");
            naoReplanejados.add(item.getId());
            return sequenciaAtual;
        }
        if (item.getOrdemTrabalhoPatioId() == null) {
            alertas.add("O item " + codigo + " nao possui ordem real no Yard.");
            naoReplanejados.add(item.getId());
            return sequenciaAtual;
        }
        if (linha == null || coluna == null || !StringUtils.hasText(reserva.getCamada())) {
            alertas.add("O item " + codigo + " possui posicao incompleta no resultado do otimizador.");
            naoReplanejados.add(item.getId());
            return sequenciaAtual;
        }
        if (!StringUtils.hasText(equipamento)) {
            alertas.add("O item " + codigo + " nao possui equipamento real atribuido pelo otimizador.");
            naoReplanejados.add(item.getId());
            return sequenciaAtual;
        }
        int novaSequencia = sequenciaAtual + 1;
        atribuicoes.put(chave, new ItemPlanoOtimizadoNavioPatioDTO(
                item.getId(),
                item.getOrdemTrabalhoPatioId(),
                item.getCodigoLote(),
                linha,
                coluna,
                reserva.getCamada(),
                equipamento,
                novaSequencia));
        return novaSequencia;
    }

    private AplicacaoPlanoYardDTO montarComandoYard(
            Long visitaId,
            String usuario,
            PlanoInterpretado plano
    ) {
        AplicacaoPlanoYardDTO comando = new AplicacaoPlanoYardDTO();
        comando.setPlanoId(plano.planoId());
        comando.setVisitaNavioId(visitaId);
        comando.setUsuario(usuario);
        comando.setItens(plano.itens().stream().map(item -> {
            ItemPlanoYardDTO dto = new ItemPlanoYardDTO();
            dto.setOrdemTrabalhoPatioId(item.ordemTrabalhoPatioId());
            dto.setItemOperacaoNavioId(item.itemOperacaoNavioId());
            dto.setCodigoConteiner(item.codigoCarga());
            dto.setLinha(item.linha());
            dto.setColuna(item.coluna());
            dto.setCamada(item.camada());
            dto.setEquipamento(item.equipamento());
            dto.setPrioridadeOperacional(item.sequenciaPlano());
            return dto;
        }).toList());
        return comando;
    }

    private List<ReservaPatioNavioDTO> reservasSugeridas(
            List<ItemPlanoOtimizadoNavioPatioDTO> itensPlano
    ) {
        Map<Long, ReservaPosicaoPatioNavio> reservas = reservasAtivasPorItem(null);
        return itensPlano.stream()
                .map(item -> reservas.get(item.itemOperacaoNavioId()))
                .filter(Objects::nonNull)
                .map(ReservaPatioNavioDTO::de)
                .toList();
    }

    private Map<Long, ReservaPosicaoPatioNavio> reservasAtivasPorItem(Long visitaId) {
        Map<Long, ReservaPosicaoPatioNavio> resultado = new HashMap<>();
        reservaRepositorio.findAll().stream()
                .filter(reserva -> visitaId == null || Objects.equals(visitaId, reserva.getVisitaNavioId()))
                .filter(reserva -> reserva.getStatus() == StatusReservaPatioNavio.ATIVA)
                .filter(reserva -> reserva.getExpiraEm() == null
                        || reserva.getExpiraEm().isAfter(LocalDateTime.now()))
                .forEach(reserva -> resultado.put(reserva.getItemOperacaoNavioId(), reserva));
        return resultado;
    }

    private int distanciaOtimizada(
            Map<String, Object> plano,
            List<ItemPlanoOtimizadoNavioPatioDTO> itensPlano
    ) {
        int distancia = listaMapas(plano.get("jobsDualCycle")).stream()
                .mapToInt(job -> inteiroOuZero(job.get("distanciaTotal")))
                .sum();
        distancia += listaMapas(plano.get("rotasEquipamento")).stream()
                .mapToInt(rota -> inteiroOuZero(rota.get("distanciaTotal")))
                .sum();
        if (distancia == 0) {
            distancia = itensPlano.stream()
                    .mapToInt(item -> Math.abs(item.linha()) + Math.abs(item.coluna()))
                    .sum();
        }
        return distancia;
    }

    private String calcularRisco(
            List<String> alertas,
            BigDecimal eficiencia,
            String status,
            int rehandlesEstimados,
            int totalEsperado
    ) {
        if (!alertas.isEmpty()) {
            return "ALTO";
        }
        if (totalEsperado == 0) {
            return "ALTO";
        }
        if (eficiencia.compareTo(BigDecimal.valueOf(30)) < 0
                || "REGULAR".equalsIgnoreCase(status)
                || "PESSIMO".equalsIgnoreCase(status)
                || rehandlesEstimados > Math.max(1, totalEsperado / 3)) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    private String gerarPlanoId(
            Long visitaId,
            List<ItemPlanoOtimizadoNavioPatioDTO> itens,
            int distanciaOriginal,
            int distanciaOtimizada
    ) {
        StringBuilder conteudo = new StringBuilder()
                .append(visitaId).append('|')
                .append(VERSAO_PLANO).append('|')
                .append(distanciaOriginal).append('|')
                .append(distanciaOtimizada);
        itens.stream()
                .sorted(Comparator.comparing(ItemPlanoOtimizadoNavioPatioDTO::sequenciaPlano))
                .forEach(item -> conteudo.append('|')
                        .append(item.itemOperacaoNavioId()).append(':')
                        .append(item.ordemTrabalhoPatioId()).append(':')
                        .append(item.linha()).append(':')
                        .append(item.coluna()).append(':')
                        .append(normalizar(item.camada())).append(':')
                        .append(normalizar(item.equipamento())).append(':')
                        .append(item.sequenciaPlano()));
        return UUID.nameUUIDFromBytes(conteudo.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<Map<String, Object>> listaMapas(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object item : lista) {
            if (item instanceof Map<?, ?> mapa) {
                Map<String, Object> convertido = new LinkedHashMap<>();
                mapa.forEach((chave, conteudo) -> convertido.put(String.valueOf(chave), conteudo));
                resultado.add(convertido);
            }
        }
        return resultado;
    }

    private Integer inteiro(Object valor) {
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(valor));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int inteiroOuZero(Object valor) {
        Integer convertido = inteiro(valor);
        return convertido == null ? 0 : convertido;
    }

    private BigDecimal decimal(Object valor) {
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(valor));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    public record ResultadoAplicacaoPlano(
            String planoId,
            int versaoPlano,
            List<ReservaPatioNavioDTO> reservas,
            BigDecimal economiaPercentual,
            String riscoRehandle,
            List<String> alertasImpeditivos,
            List<Long> itensNaoReplanejados,
            int distanciaOriginal,
            int distanciaOtimizada
    ) {
    }

    private record PlanoInterpretado(
            String planoId,
            List<ItemPlanoOtimizadoNavioPatioDTO> itens,
            BigDecimal economiaPercentual,
            String riscoRehandle,
            List<String> alertasImpeditivos,
            List<Long> itensNaoReplanejados,
            int distanciaOriginal,
            int distanciaOtimizada
    ) {
    }
}
