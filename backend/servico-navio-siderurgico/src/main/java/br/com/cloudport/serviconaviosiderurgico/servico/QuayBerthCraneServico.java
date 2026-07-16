package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoGuindasteVisita;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.AlocacaoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.GuindasteQuayMonitorDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ProdutividadeCaisDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.QuayMonitorDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoGuindasteVisitaRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuayBerthCraneServico {

    private static final BigDecimal SESSENTA = BigDecimal.valueOf(60);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final VisitaNavioServico visitaNavioServico;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final PlanoGuindasteVisitaRepositorio planoRepositorio;

    public QuayBerthCraneServico(
            VisitaNavioServico visitaNavioServico,
            ItemOperacaoNavioRepositorio itemRepositorio,
            PlanoGuindasteVisitaRepositorio planoRepositorio
    ) {
        this.visitaNavioServico = visitaNavioServico;
        this.itemRepositorio = itemRepositorio;
        this.planoRepositorio = planoRepositorio;
    }

    @Transactional
    public PlanoGuindasteDTO salvarPlano(Long visitaId, ComandoPlanoGuindasteDTO comando) {
        VisitaNavio visita = visitaNavioServico.buscarEntidade(visitaId);
        visitaNavioServico.validarVisitaEditavel(visita);
        validarComando(comando, itemRepositorio.findByVisitaNavioId(visitaId));

        String berco = normalizarObrigatorio(comando.berco(), "Berco e obrigatorio.");
        String usuario = normalizarUsuario(comando.usuario());
        String observacao = limpar(comando.observacao());

        planoRepositorio.deleteByVisitaNavioId(visitaId);
        List<PlanoGuindasteVisita> entidades = comando.guindastes().stream()
                .sorted(Comparator.comparing(AlocacaoGuindasteDTO::sequencia))
                .map(dto -> criarEntidade(visita, dto, comando.status(), berco, usuario, observacao))
                .collect(Collectors.toList());
        List<PlanoGuindasteVisita> salvos = planoRepositorio.saveAll(entidades);

        visitaNavioServico.registrarEvento(
                visita,
                null,
                "CRANE_PLAN_ATUALIZADO",
                "Plano de guindastes atualizado no berco " + berco + " com " + salvos.size() + " alocacao(oes).",
                usuario,
                null,
                comando.status().name());

        return montarPlanoDTO(visita, salvos);
    }

    @Transactional(readOnly = true)
    public PlanoGuindasteDTO obterPlano(Long visitaId) {
        VisitaNavio visita = visitaNavioServico.buscarEntidade(visitaId);
        return montarPlanoDTO(visita, planoRepositorio.findByVisitaNavioIdOrderBySequenciaAsc(visitaId));
    }

    @Transactional(readOnly = true)
    public QuayMonitorDTO obterQuayMonitor(Long visitaId) {
        VisitaNavio visita = visitaNavioServico.buscarEntidade(visitaId);
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioId(visitaId);
        List<PlanoGuindasteVisita> planos = planoRepositorio.findByVisitaNavioIdOrderBySequenciaAsc(visitaId);
        ProdutividadeCaisDTO produtividade = calcularProdutividade(visita, itens, planos);
        List<String> alertas = calcularAlertas(visita, itens, planos, produtividade);
        StatusPlanoGuindaste statusPlano = planos.isEmpty() ? null : planos.get(0).getStatus();

        return new QuayMonitorDTO(
                visita.getId(),
                visita.getCodigoVisita(),
                visita.getNavio().getId(),
                visita.getNavio().getNome(),
                visita.getFase(),
                visita.getBercoPrevisto(),
                visita.getBercoAtual(),
                visita.getEta(),
                visita.getEtb(),
                visita.getAtb(),
                visita.getInicioOperacao(),
                visita.getFimOperacao(),
                visita.getEtd(),
                visita.getAtd(),
                statusPlano,
                produtividade,
                alertas);
    }

    @Transactional(readOnly = true)
    public ProdutividadeCaisDTO obterProdutividadeCais(Long visitaId) {
        VisitaNavio visita = visitaNavioServico.buscarEntidade(visitaId);
        return calcularProdutividade(
                visita,
                itemRepositorio.findByVisitaNavioId(visitaId),
                planoRepositorio.findByVisitaNavioIdOrderBySequenciaAsc(visitaId));
    }

    private ProdutividadeCaisDTO calcularProdutividade(
            VisitaNavio visita,
            List<ItemOperacaoNavio> itens,
            List<PlanoGuindasteVisita> planos
    ) {
        int movimentosDosItens = itens.stream().mapToInt(this::quantidade).sum();
        int movimentosDoPlano = planos.stream().mapToInt(PlanoGuindasteVisita::getMovimentosPlanejados).sum();
        int movimentosPlanejados = Math.max(movimentosDosItens, movimentosDoPlano);
        int movimentosRealizados = itens.stream()
                .filter(item -> item.getStatus() == StatusItemCarga.OPERADO)
                .mapToInt(this::quantidade)
                .sum();
        int movimentosPendentes = Math.max(0, movimentosPlanejados - movimentosRealizados);

        LocalDateTime fimReferencia = visita.getInicioOperacao() == null
                ? null
                : (visita.getFimOperacao() == null ? LocalDateTime.now() : visita.getFimOperacao());
        Long minutosOperacao = visita.getInicioOperacao() == null
                ? null
                : Math.max(1L, Duration.between(visita.getInicioOperacao(), fimReferencia).toMinutes());

        BigDecimal produtividadeAtual = calcularProdutividade(movimentosRealizados, minutosOperacao);
        BigDecimal produtividadePlanejada = planos.stream()
                .map(PlanoGuindasteVisita::getProdutividadePlanejadaMovimentosHora)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = calcularPercentual(movimentosRealizados, movimentosPlanejados);
        BigDecimal taxaProjecao = produtividadeAtual.signum() > 0 ? produtividadeAtual : produtividadePlanejada;
        LocalDateTime previsaoTermino = projetarTermino(movimentosPendentes, taxaProjecao, LocalDateTime.now());

        Map<Long, Integer> realizadosPorPlano = distribuirMovimentosRealizados(itens, planos);
        List<GuindasteQuayMonitorDTO> guindastes = planos.stream()
                .map(plano -> montarGuindaste(plano, realizadosPorPlano.getOrDefault(plano.getId(), 0), minutosOperacao))
                .collect(Collectors.toList());

        return new ProdutividadeCaisDTO(
                visita.getId(),
                visita.getCodigoVisita(),
                resolverBerco(visita, planos),
                visita.getInicioOperacao(),
                fimReferencia,
                minutosOperacao,
                movimentosPlanejados,
                movimentosRealizados,
                movimentosPendentes,
                produtividadePlanejada,
                produtividadeAtual,
                percentual,
                previsaoTermino,
                guindastes);
    }

    private GuindasteQuayMonitorDTO montarGuindaste(
            PlanoGuindasteVisita plano,
            int movimentosRealizados,
            Long minutosOperacao
    ) {
        int movimentosPendentes = Math.max(0, plano.getMovimentosPlanejados() - movimentosRealizados);
        BigDecimal produtividadeAtual = calcularProdutividade(movimentosRealizados, minutosOperacao);
        BigDecimal percentual = calcularPercentual(movimentosRealizados, plano.getMovimentosPlanejados());
        BigDecimal taxaProjecao = produtividadeAtual.signum() > 0
                ? produtividadeAtual
                : plano.getProdutividadePlanejadaMovimentosHora();
        LocalDateTime baseProjecao = LocalDateTime.now().isBefore(plano.getInicioPlanejado())
                ? plano.getInicioPlanejado()
                : LocalDateTime.now();
        LocalDateTime previsaoTermino = projetarTermino(movimentosPendentes, taxaProjecao, baseProjecao);
        String statusOperacional = determinarStatusOperacional(plano, movimentosRealizados);

        return new GuindasteQuayMonitorDTO(
                plano.getId(),
                plano.getCodigoGuindaste(),
                plano.getRecursoCais(),
                plano.getPorao(),
                plano.getWorkQueueId(),
                plano.getSequencia(),
                plano.getMovimentosPlanejados(),
                movimentosRealizados,
                movimentosPendentes,
                plano.getProdutividadePlanejadaMovimentosHora(),
                produtividadeAtual,
                percentual,
                plano.getInicioPlanejado(),
                plano.getFimPlanejado(),
                previsaoTermino,
                statusOperacional);
    }

    private Map<Long, Integer> distribuirMovimentosRealizados(
            List<ItemOperacaoNavio> itens,
            List<PlanoGuindasteVisita> planos
    ) {
        Map<Integer, Integer> movimentosPorPorao = new HashMap<>();
        itens.stream()
                .filter(item -> item.getStatus() == StatusItemCarga.OPERADO)
                .forEach(item -> {
                    Integer porao = item.getPoraoReal() == null ? item.getPoraoPlanejado() : item.getPoraoReal();
                    if (porao != null) {
                        movimentosPorPorao.merge(porao, quantidade(item), Integer::sum);
                    }
                });

        Map<Integer, List<PlanoGuindasteVisita>> planosPorPorao = planos.stream()
                .collect(Collectors.groupingBy(
                        PlanoGuindasteVisita::getPorao,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, Integer> resultado = new HashMap<>();

        planosPorPorao.forEach((porao, planosDoPorao) -> {
            int restante = movimentosPorPorao.getOrDefault(porao, 0);
            List<PlanoGuindasteVisita> ordenados = planosDoPorao.stream()
                    .sorted(Comparator.comparing(PlanoGuindasteVisita::getSequencia))
                    .collect(Collectors.toList());
            for (int indice = 0; indice < ordenados.size(); indice++) {
                PlanoGuindasteVisita plano = ordenados.get(indice);
                boolean ultimo = indice == ordenados.size() - 1;
                int realizados = ultimo
                        ? restante
                        : Math.min(restante, plano.getMovimentosPlanejados());
                resultado.put(plano.getId(), Math.max(0, realizados));
                restante = Math.max(0, restante - realizados);
            }
        });
        return resultado;
    }

    private List<String> calcularAlertas(
            VisitaNavio visita,
            List<ItemOperacaoNavio> itens,
            List<PlanoGuindasteVisita> planos,
            ProdutividadeCaisDTO produtividade
    ) {
        List<String> alertas = new ArrayList<>();
        String bercoVisita = visita.getBercoAtual() == null ? visita.getBercoPrevisto() : visita.getBercoAtual();
        if (bercoVisita == null || bercoVisita.isBlank()) {
            alertas.add("Visita sem berco previsto ou atual.");
        }
        if (planos.isEmpty()) {
            alertas.add("Visita sem plano de guindastes.");
            return alertas;
        }

        String bercoPlano = planos.get(0).getBerco();
        if (bercoVisita != null && !bercoVisita.equalsIgnoreCase(bercoPlano)) {
            alertas.add("Berco do plano de guindastes diverge do berco da visita.");
        }
        if (planos.stream().anyMatch(plano -> plano.getWorkQueueId() == null)) {
            alertas.add("Existem alocacoes de guindaste sem work queue associada.");
        }
        int movimentosDoPlano = planos.stream().mapToInt(PlanoGuindasteVisita::getMovimentosPlanejados).sum();
        int movimentosDosItens = itens.stream().mapToInt(this::quantidade).sum();
        if (movimentosDoPlano < movimentosDosItens) {
            alertas.add("Plano de guindastes nao cobre todos os movimentos planejados da visita.");
        }
        Set<Integer> poroesPlanejados = planos.stream().map(PlanoGuindasteVisita::getPorao).collect(Collectors.toSet());
        long itensSemPoraoCoberto = itens.stream()
                .filter(item -> item.getPoraoPlanejado() == null || !poroesPlanejados.contains(item.getPoraoPlanejado()))
                .count();
        if (itensSemPoraoCoberto > 0) {
            alertas.add(itensSemPoraoCoberto + " item(ns) sem porao coberto pelo plano de guindastes.");
        }
        LocalDateTime agora = LocalDateTime.now();
        long guindastesAtrasados = produtividade.guindastes().stream()
                .filter(guindaste -> "ATRASADO".equals(guindaste.statusOperacional()))
                .count();
        if (guindastesAtrasados > 0) {
            alertas.add(guindastesAtrasados + " alocacao(oes) de guindaste ultrapassaram o fim planejado.");
        }
        if (visita.getEtd() != null && agora.isAfter(visita.getEtd()) && produtividade.movimentosPendentes() > 0) {
            alertas.add("ETD ultrapassado com movimentos de cais pendentes.");
        }
        return alertas;
    }

    private void validarComando(ComandoPlanoGuindasteDTO comando, List<ItemOperacaoNavio> itens) {
        if (comando == null) {
            throw new IllegalArgumentException("Plano de guindastes e obrigatorio.");
        }
        normalizarObrigatorio(comando.berco(), "Berco e obrigatorio.");
        if (comando.status() == null) {
            throw new IllegalArgumentException("Status do plano e obrigatorio.");
        }
        if (comando.guindastes() == null || comando.guindastes().isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma alocacao de guindaste.");
        }

        Set<Integer> sequencias = new HashSet<>();
        Set<Integer> poroesConhecidos = itens.stream()
                .map(ItemOperacaoNavio::getPoraoPlanejado)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, List<AlocacaoGuindasteDTO>> porGuindaste = new HashMap<>();

        for (AlocacaoGuindasteDTO alocacao : comando.guindastes()) {
            validarAlocacao(alocacao, sequencias, poroesConhecidos);
            String codigo = normalizarObrigatorio(alocacao.codigoGuindaste(), "Codigo do guindaste e obrigatorio.");
            porGuindaste.computeIfAbsent(codigo, chave -> new ArrayList<>()).add(alocacao);
        }

        porGuindaste.forEach((codigo, alocacoes) -> {
            List<AlocacaoGuindasteDTO> ordenadas = alocacoes.stream()
                    .sorted(Comparator.comparing(AlocacaoGuindasteDTO::inicioPlanejado))
                    .collect(Collectors.toList());
            for (int indice = 1; indice < ordenadas.size(); indice++) {
                AlocacaoGuindasteDTO anterior = ordenadas.get(indice - 1);
                AlocacaoGuindasteDTO atual = ordenadas.get(indice);
                if (atual.inicioPlanejado().isBefore(anterior.fimPlanejado())) {
                    throw new IllegalArgumentException("Alocacoes sobrepostas para o guindaste " + codigo + ".");
                }
            }
        });
    }

    private void validarAlocacao(
            AlocacaoGuindasteDTO alocacao,
            Set<Integer> sequencias,
            Set<Integer> poroesConhecidos
    ) {
        if (alocacao == null) {
            throw new IllegalArgumentException("Alocacao de guindaste invalida.");
        }
        normalizarObrigatorio(alocacao.codigoGuindaste(), "Codigo do guindaste e obrigatorio.");
        if (alocacao.porao() == null || alocacao.porao() <= 0) {
            throw new IllegalArgumentException("Porao deve ser maior que zero.");
        }
        if (!poroesConhecidos.isEmpty() && !poroesConhecidos.contains(alocacao.porao())) {
            throw new IllegalArgumentException("Porao " + alocacao.porao() + " nao existe nos itens planejados da visita.");
        }
        if (alocacao.workQueueId() != null && alocacao.workQueueId() <= 0) {
            throw new IllegalArgumentException("Work queue deve possuir identificador valido.");
        }
        if (alocacao.sequencia() == null || alocacao.sequencia() <= 0 || !sequencias.add(alocacao.sequencia())) {
            throw new IllegalArgumentException("Sequencia do plano deve ser positiva e unica.");
        }
        if (alocacao.movimentosPlanejados() == null || alocacao.movimentosPlanejados() <= 0) {
            throw new IllegalArgumentException("Movimentos planejados devem ser maiores que zero.");
        }
        if (alocacao.produtividadePlanejadaMovimentosHora() == null
                || alocacao.produtividadePlanejadaMovimentosHora().signum() <= 0) {
            throw new IllegalArgumentException("Produtividade planejada deve ser maior que zero.");
        }
        if (alocacao.inicioPlanejado() == null || alocacao.fimPlanejado() == null
                || !alocacao.fimPlanejado().isAfter(alocacao.inicioPlanejado())) {
            throw new IllegalArgumentException("Fim planejado deve ser posterior ao inicio planejado.");
        }
    }

    private PlanoGuindasteVisita criarEntidade(
            VisitaNavio visita,
            AlocacaoGuindasteDTO dto,
            StatusPlanoGuindaste status,
            String berco,
            String usuario,
            String observacaoPlano
    ) {
        PlanoGuindasteVisita plano = new PlanoGuindasteVisita();
        plano.setVisitaNavio(visita);
        plano.setCodigoGuindaste(normalizarObrigatorio(dto.codigoGuindaste(), "Codigo do guindaste e obrigatorio."));
        plano.setRecursoCais(limpar(dto.recursoCais()));
        plano.setPorao(dto.porao());
        plano.setWorkQueueId(dto.workQueueId());
        plano.setSequencia(dto.sequencia());
        plano.setMovimentosPlanejados(dto.movimentosPlanejados());
        plano.setProdutividadePlanejadaMovimentosHora(dto.produtividadePlanejadaMovimentosHora().setScale(2, RoundingMode.HALF_UP));
        plano.setInicioPlanejado(dto.inicioPlanejado());
        plano.setFimPlanejado(dto.fimPlanejado());
        plano.setStatus(status);
        plano.setBerco(berco);
        plano.setUsuario(usuario);
        plano.setObservacao(limpar(dto.observacao()) == null ? observacaoPlano : limpar(dto.observacao()));
        return plano;
    }

    private PlanoGuindasteDTO montarPlanoDTO(VisitaNavio visita, List<PlanoGuindasteVisita> planos) {
        if (planos.isEmpty()) {
            return new PlanoGuindasteDTO(
                    visita.getId(),
                    visita.getBercoAtual() == null ? visita.getBercoPrevisto() : visita.getBercoAtual(),
                    null,
                    null,
                    null,
                    null,
                    List.of());
        }
        PlanoGuindasteVisita primeiro = planos.get(0);
        LocalDateTime atualizadoEm = planos.stream()
                .map(PlanoGuindasteVisita::getAtualizadoEm)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        List<AlocacaoGuindasteDTO> alocacoes = planos.stream()
                .map(plano -> new AlocacaoGuindasteDTO(
                        plano.getId(),
                        plano.getCodigoGuindaste(),
                        plano.getRecursoCais(),
                        plano.getPorao(),
                        plano.getWorkQueueId(),
                        plano.getSequencia(),
                        plano.getMovimentosPlanejados(),
                        plano.getProdutividadePlanejadaMovimentosHora(),
                        plano.getInicioPlanejado(),
                        plano.getFimPlanejado(),
                        plano.getObservacao()))
                .collect(Collectors.toList());
        return new PlanoGuindasteDTO(
                visita.getId(),
                primeiro.getBerco(),
                primeiro.getStatus(),
                primeiro.getUsuario(),
                primeiro.getObservacao(),
                atualizadoEm,
                alocacoes);
    }

    private BigDecimal calcularProdutividade(int movimentos, Long minutos) {
        if (movimentos <= 0 || minutos == null || minutos <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(SESSENTA, 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(movimentos).divide(horas, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPercentual(int realizado, int planejado) {
        if (planejado <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(realizado)
                .multiply(CEM)
                .divide(BigDecimal.valueOf(planejado), 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime projetarTermino(int movimentosPendentes, BigDecimal produtividade, LocalDateTime base) {
        if (movimentosPendentes <= 0) {
            return base;
        }
        if (produtividade == null || produtividade.signum() <= 0) {
            return null;
        }
        long minutos = BigDecimal.valueOf(movimentosPendentes)
                .multiply(SESSENTA)
                .divide(produtividade, 0, RoundingMode.CEILING)
                .longValue();
        return base.plusMinutes(Math.max(1L, minutos));
    }

    private String determinarStatusOperacional(PlanoGuindasteVisita plano, int realizados) {
        if (realizados >= plano.getMovimentosPlanejados()) {
            return "CONCLUIDO";
        }
        LocalDateTime agora = LocalDateTime.now();
        if (agora.isAfter(plano.getFimPlanejado())) {
            return "ATRASADO";
        }
        if (realizados > 0 || !agora.isBefore(plano.getInicioPlanejado())) {
            return "EM_EXECUCAO";
        }
        return "AGUARDANDO";
    }

    private int quantidade(ItemOperacaoNavio item) {
        return item.getQuantidade() == null ? 0 : Math.max(0, item.getQuantidade());
    }

    private String resolverBerco(VisitaNavio visita, List<PlanoGuindasteVisita> planos) {
        if (!planos.isEmpty()) {
            return planos.get(0).getBerco();
        }
        return visita.getBercoAtual() == null ? visita.getBercoPrevisto() : visita.getBercoAtual();
    }

    private String normalizarObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarUsuario(String usuario) {
        return usuario == null || usuario.isBlank() ? "sistema" : usuario.trim();
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
