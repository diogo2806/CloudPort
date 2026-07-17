package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.comum.constantes.YardConstants;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.util.StringUtils;

@Service
public class OtimizadorPesquisaOperacionalPatioServico {

    private static final long CUSTO_NAO_ALOCADO = 1_000_000L;
    private static final long CUSTO_PROIBIDO = 10_000_000L;
    private static final long CUSTO_MAXIMO_VALIDO = 900_000L;
    private static final int PESO_REHANDLE = 10_000;
    private static final int PESO_DESTINO = 1_000;
    private static final int PESO_TIPO_CARGA = 500;
    private static final int PESO_DISTANCIA_EQUIPAMENTO = 25;
    private static final int PESO_ABERTURA_PILHA = 80;
    private static final int PESO_CAMADA = 20;
    private static final int PESO_OCUPACAO_BLOCO = 2;

    public ResultadoOtimizacao otimizar(
            List<ConteinerPatio> candidatos,
            List<PosicaoPatio> posicoes,
            List<EquipamentoPatio> equipamentos,
            List<ConteinerPatio> inventario
    ) {
        List<ConteinerPatio> candidatosOrdenados = candidatos == null
                ? List.of()
                : candidatos.stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator
                                .comparing((ConteinerPatio conteiner) -> normalizar(conteiner.getDestino()))
                                .thenComparing(ConteinerPatio::getCodigo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                .thenComparing(ConteinerPatio::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
        if (candidatosOrdenados.isEmpty()) {
            return new ResultadoOtimizacao(Map.of(), Map.of());
        }

        EstadoOtimizacao estado = new EstadoOtimizacao(
                posicoes == null ? List.of() : posicoes,
                equipamentos == null ? List.of() : equipamentos,
                inventario == null ? List.of() : inventario);
        Map<ConteinerPatio, PosicaoPatio> alocacoes = new LinkedHashMap<>();
        Map<ConteinerPatio, String> motivos = new LinkedHashMap<>();

        List<ConteinerPatio> perigosos = candidatosOrdenados.stream()
                .filter(this::ehCargaPerigosa)
                .toList();
        for (ConteinerPatio conteiner : perigosos) {
            PosicaoPatio posicao = selecionarMelhorPosicaoIndividual(conteiner, estado);
            if (posicao == null) {
                motivos.put(conteiner, "sem posição segura que respeite isolamento de carga perigosa");
                continue;
            }
            registrarAlocacao(conteiner, posicao, estado, alocacoes);
        }

        List<ConteinerPatio> pendentes = candidatosOrdenados.stream()
                .filter(conteiner -> !ehCargaPerigosa(conteiner))
                .collect(Collectors.toCollection(ArrayList::new));
        while (!pendentes.isEmpty()) {
            List<PosicaoPatio> posicoesCandidatas = estado.proximasPosicoesLivres();
            if (posicoesCandidatas.isEmpty()) {
                break;
            }

            Map<ConteinerPatio, PosicaoPatio> atribuicoes = resolverAtribuicaoMinimoCusto(
                    pendentes,
                    posicoesCandidatas,
                    estado);
            if (atribuicoes.isEmpty()) {
                break;
            }

            atribuicoes.forEach((conteiner, posicao) ->
                    registrarAlocacao(conteiner, posicao, estado, alocacoes));
            pendentes.removeAll(atribuicoes.keySet());
        }

        for (ConteinerPatio conteiner : pendentes) {
            motivos.put(conteiner, diagnosticarNaoAlocacao(conteiner, estado));
        }
        return new ResultadoOtimizacao(alocacoes, motivos);
    }

    private Map<ConteinerPatio, PosicaoPatio> resolverAtribuicaoMinimoCusto(
            List<ConteinerPatio> conteineres,
            List<PosicaoPatio> posicoes,
            EstadoOtimizacao estado
    ) {
        int totalConteineres = conteineres.size();
        int totalPosicoesReais = posicoes.size();
        int totalColunas = totalPosicoesReais + totalConteineres;
        long[][] custos = new long[totalConteineres][totalColunas];

        for (int indiceConteiner = 0; indiceConteiner < totalConteineres; indiceConteiner++) {
            ConteinerPatio conteiner = conteineres.get(indiceConteiner);
            for (int indicePosicao = 0; indicePosicao < totalPosicoesReais; indicePosicao++) {
                PosicaoPatio posicao = posicoes.get(indicePosicao);
                if (!ehPosicaoCompativel(conteiner, posicao, estado)) {
                    custos[indiceConteiner][indicePosicao] = CUSTO_PROIBIDO;
                    continue;
                }
                custos[indiceConteiner][indicePosicao] = Math.min(
                        CUSTO_MAXIMO_VALIDO,
                        calcularCusto(conteiner, posicao, estado) + indicePosicao);
            }
            for (int indiceDummy = totalPosicoesReais; indiceDummy < totalColunas; indiceDummy++) {
                custos[indiceConteiner][indiceDummy] = CUSTO_NAO_ALOCADO + indiceDummy;
            }
        }

        int[] atribuicao = resolverHungaro(custos);
        Map<ConteinerPatio, PosicaoPatio> resultado = new LinkedHashMap<>();
        Set<Long> idsPosicoesUsadas = new HashSet<>();
        for (int indiceConteiner = 0; indiceConteiner < atribuicao.length; indiceConteiner++) {
            int indicePosicao = atribuicao[indiceConteiner];
            if (indicePosicao < 0 || indicePosicao >= totalPosicoesReais) {
                continue;
            }
            if (custos[indiceConteiner][indicePosicao] >= CUSTO_NAO_ALOCADO) {
                continue;
            }
            PosicaoPatio posicao = posicoes.get(indicePosicao);
            if (posicao.getId() == null || !idsPosicoesUsadas.add(posicao.getId())) {
                continue;
            }
            resultado.put(conteineres.get(indiceConteiner), posicao);
        }
        return resultado;
    }

    private int[] resolverHungaro(long[][] custos) {
        int linhas = custos.length;
        int colunas = custos[0].length;
        long[] potencialLinhas = new long[linhas + 1];
        long[] potencialColunas = new long[colunas + 1];
        int[] linhaPorColuna = new int[colunas + 1];
        int[] caminho = new int[colunas + 1];

        for (int linha = 1; linha <= linhas; linha++) {
            linhaPorColuna[0] = linha;
            int colunaAtual = 0;
            long[] menorCusto = new long[colunas + 1];
            Arrays.fill(menorCusto, Long.MAX_VALUE / 4);
            boolean[] usada = new boolean[colunas + 1];

            do {
                usada[colunaAtual] = true;
                int linhaAtual = linhaPorColuna[colunaAtual];
                long delta = Long.MAX_VALUE / 4;
                int proximaColuna = 0;
                for (int coluna = 1; coluna <= colunas; coluna++) {
                    if (usada[coluna]) {
                        continue;
                    }
                    long custoReduzido = custos[linhaAtual - 1][coluna - 1]
                            - potencialLinhas[linhaAtual]
                            - potencialColunas[coluna];
                    if (custoReduzido < menorCusto[coluna]) {
                        menorCusto[coluna] = custoReduzido;
                        caminho[coluna] = colunaAtual;
                    }
                    if (menorCusto[coluna] < delta) {
                        delta = menorCusto[coluna];
                        proximaColuna = coluna;
                    }
                }
                for (int coluna = 0; coluna <= colunas; coluna++) {
                    if (usada[coluna]) {
                        potencialLinhas[linhaPorColuna[coluna]] += delta;
                        potencialColunas[coluna] -= delta;
                    } else if (coluna > 0) {
                        menorCusto[coluna] -= delta;
                    }
                }
                colunaAtual = proximaColuna;
            } while (linhaPorColuna[colunaAtual] != 0);

            do {
                int colunaAnterior = caminho[colunaAtual];
                linhaPorColuna[colunaAtual] = linhaPorColuna[colunaAnterior];
                colunaAtual = colunaAnterior;
            } while (colunaAtual != 0);
        }

        int[] atribuicao = new int[linhas];
        Arrays.fill(atribuicao, -1);
        for (int coluna = 1; coluna <= colunas; coluna++) {
            if (linhaPorColuna[coluna] > 0) {
                atribuicao[linhaPorColuna[coluna] - 1] = coluna - 1;
            }
        }
        return atribuicao;
    }

    private PosicaoPatio selecionarMelhorPosicaoIndividual(
            ConteinerPatio conteiner,
            EstadoOtimizacao estado
    ) {
        return estado.proximasPosicoesLivres().stream()
                .filter(posicao -> ehPosicaoCompativel(conteiner, posicao, estado))
                .min(Comparator
                        .comparingLong((PosicaoPatio posicao) -> calcularCusto(conteiner, posicao, estado))
                        .thenComparing(PosicaoPatio::getLinha)
                        .thenComparing(PosicaoPatio::getColuna)
                        .thenComparing(posicao -> nivel(posicao.getCamadaOperacional()))
                        .thenComparing(PosicaoPatio::getId))
                .orElse(null);
    }

    private boolean ehPosicaoCompativel(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            EstadoOtimizacao estado
    ) {
        if (posicao.getId() == null
                || estado.posicoesOcupadas.contains(posicao.getId())
                || posicao.isBloqueada()
                || posicao.isInterditada()
                || !posicao.isAreaPermitida()) {
            return false;
        }
        if (posicao.possuiReservaAtiva(estado.referencia)
                && !Objects.equals(normalizar(posicao.getReservaCodigoConteiner()), normalizar(conteiner.getCodigo()))) {
            return false;
        }
        if (!tipoCargaPermitido(conteiner, posicao)) {
            return false;
        }
        if (!pesoPermitido(conteiner, posicao)) {
            return false;
        }
        if (!camadaPermitida(conteiner, posicao)) {
            return false;
        }
        if (ehCargaRefrigerada(conteiner) && !estado.possuiCoberturaReefer(posicao)) {
            return false;
        }
        return !ehCargaPerigosa(conteiner) || estado.possuiIsolamentoPerigoso(posicao);
    }

    private boolean tipoCargaPermitido(ConteinerPatio conteiner, PosicaoPatio posicao) {
        if (!StringUtils.hasText(posicao.getTiposCargaPermitidos())) {
            return true;
        }
        String tipo = tipoCarga(conteiner).name();
        return Arrays.stream(posicao.getTiposCargaPermitidos().toUpperCase(Locale.ROOT).split("[,;|\\s]+"))
                .map(String::trim)
                .anyMatch(valor -> valor.equals(tipo));
    }

    private boolean pesoPermitido(ConteinerPatio conteiner, PosicaoPatio posicao) {
        BigDecimal peso = conteiner.getPesoToneladas();
        return peso == null
                || posicao.getPesoMaximoToneladas() == null
                || peso.compareTo(posicao.getPesoMaximoToneladas()) <= 0;
    }

    private boolean camadaPermitida(ConteinerPatio conteiner, PosicaoPatio posicao) {
        int camada = nivel(posicao.getCamadaOperacional());
        if (camada <= 0) {
            return false;
        }
        int limite = YardConstants.EMPILHAMENTO_MAXIMO;
        if (posicao.getCamadaMaxima() != null) {
            limite = Math.min(limite, posicao.getCamadaMaxima());
        }
        if (posicao.getCapacidadePilha() != null) {
            limite = Math.min(limite, posicao.getCapacidadePilha());
        }
        if (conteiner.getPesoToneladas() != null) {
            if (conteiner.getPesoToneladas().compareTo(YardConstants.PESO_LIMITE_PILHA_INTERMEDIARIA) >= 0) {
                limite = Math.min(limite, 1);
            } else if (conteiner.getPesoToneladas().compareTo(YardConstants.PESO_LIMITE_PILHA_DUPLA) >= 0) {
                limite = Math.min(limite, 2);
            }
        }
        return camada <= limite;
    }

    private long calcularCusto(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            EstadoOtimizacao estado
    ) {
        long custo = 0L;
        custo += (long) calcularRiscoRehandle(conteiner, posicao, estado) * PESO_REHANDLE;
        custo += (long) calcularPenalidadeDestino(conteiner, posicao) * PESO_DESTINO;
        custo += (long) calcularMisturaTipoCarga(conteiner, posicao, estado) * PESO_TIPO_CARGA;
        custo += (long) estado.distanciaEquipamento(posicao) * PESO_DISTANCIA_EQUIPAMENTO;
        custo += estado.pilhaVazia(posicao) ? PESO_ABERTURA_PILHA : 0;
        custo += (long) nivel(posicao.getCamadaOperacional()) * PESO_CAMADA;
        custo += (long) estado.ocupacaoBloco(posicao.getBloco()) * PESO_OCUPACAO_BLOCO;
        return Math.max(0L, custo);
    }

    private int calcularRiscoRehandle(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            EstadoOtimizacao estado
    ) {
        String destino = normalizar(conteiner.getDestino());
        int risco = 0;
        for (ConteinerPatio ocupante : estado.ocupantesDaPilha(posicao)) {
            if (!Objects.equals(destino, normalizar(ocupante.getDestino()))) {
                risco += ocupante.getStatus() == StatusConteiner.AGUARDANDO_RETIRADA ? 3 : 1;
            }
        }
        return risco;
    }

    private int calcularPenalidadeDestino(ConteinerPatio conteiner, PosicaoPatio posicao) {
        String destino = normalizar(conteiner.getDestino());
        String bloco = normalizar(posicao.getBloco());
        if (destino.isEmpty() || bloco.isEmpty()) {
            return 1;
        }
        return destino.equals(bloco) || destino.contains(bloco) || bloco.contains(destino) ? 0 : 1;
    }

    private int calcularMisturaTipoCarga(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            EstadoOtimizacao estado
    ) {
        TipoCargaConteiner tipo = tipoCarga(conteiner);
        return (int) estado.ocupantesDaPilha(posicao).stream()
                .filter(ocupante -> tipoCarga(ocupante) != tipo)
                .count();
    }

    private String diagnosticarNaoAlocacao(ConteinerPatio conteiner, EstadoOtimizacao estado) {
        List<PosicaoPatio> proximas = estado.proximasPosicoesLivres();
        if (proximas.isEmpty()) {
            return "sem posição livre com apoio físico válido na pilha";
        }
        if (proximas.stream().noneMatch(posicao -> !posicao.isBloqueada()
                && !posicao.isInterditada()
                && posicao.isAreaPermitida())) {
            return "todas as próximas posições estão bloqueadas, interditadas ou fora da área permitida";
        }
        if (proximas.stream().noneMatch(posicao -> tipoCargaPermitido(conteiner, posicao))) {
            return "nenhuma posição aceita o tipo de carga";
        }
        if (proximas.stream().noneMatch(posicao -> pesoPermitido(conteiner, posicao)
                && camadaPermitida(conteiner, posicao))) {
            return "nenhuma posição respeita peso, capacidade e altura de empilhamento";
        }
        if (ehCargaRefrigerada(conteiner)
                && proximas.stream().noneMatch(estado::possuiCoberturaReefer)) {
            return "sem posição reefer com cobertura operacional disponível";
        }
        return "sem posição operacional compatível após a otimização global";
    }

    private void registrarAlocacao(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            EstadoOtimizacao estado,
            Map<ConteinerPatio, PosicaoPatio> alocacoes
    ) {
        alocacoes.put(conteiner, posicao);
        estado.registrar(conteiner, posicao);
    }

    private boolean ehCargaRefrigerada(ConteinerPatio conteiner) {
        return tipoCarga(conteiner) == TipoCargaConteiner.REFRIGERADO;
    }

    private boolean ehCargaPerigosa(ConteinerPatio conteiner) {
        return tipoCarga(conteiner) == TipoCargaConteiner.PERIGOSO;
    }

    private TipoCargaConteiner tipoCarga(ConteinerPatio conteiner) {
        if (conteiner.getTipoCarga() != null) {
            return conteiner.getTipoCarga();
        }
        String descricao = conteiner.getCarga() == null ? "" : normalizar(conteiner.getCarga().getDescricao());
        if (descricao.contains("REEFER") || descricao.contains("REFRIGER")) {
            return TipoCargaConteiner.REFRIGERADO;
        }
        if (descricao.contains("PERIG") || descricao.contains("IMO")) {
            return TipoCargaConteiner.PERIGOSO;
        }
        if (descricao.contains("GRANEL")) {
            return TipoCargaConteiner.GRANELEIRO;
        }
        return TipoCargaConteiner.OUTRO;
    }

    private int nivel(String camadaOperacional) {
        if (!StringUtils.hasText(camadaOperacional)) {
            return -1;
        }
        String somenteDigitos = camadaOperacional.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(somenteDigitos)) {
            return -1;
        }
        try {
            return Integer.parseInt(somenteDigitos);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    public static final class ResultadoOtimizacao {
        private final Map<ConteinerPatio, PosicaoPatio> alocacoes;
        private final Map<ConteinerPatio, String> motivosNaoAlocacao;

        private ResultadoOtimizacao(
                Map<ConteinerPatio, PosicaoPatio> alocacoes,
                Map<ConteinerPatio, String> motivosNaoAlocacao
        ) {
            this.alocacoes = Map.copyOf(alocacoes);
            this.motivosNaoAlocacao = Map.copyOf(motivosNaoAlocacao);
        }

        public Map<ConteinerPatio, PosicaoPatio> getAlocacoes() {
            return alocacoes;
        }

        public Map<ConteinerPatio, String> getMotivosNaoAlocacao() {
            return motivosNaoAlocacao;
        }
    }

    private final class EstadoOtimizacao {
        private final List<PosicaoPatio> posicoes;
        private final List<EquipamentoPatio> equipamentosOperacionais;
        private final Map<Long, ConteinerPatio> ocupantePorPosicao = new HashMap<>();
        private final Set<Long> posicoesOcupadas = new HashSet<>();
        private final LocalDateTime referencia = LocalDateTime.now();

        private EstadoOtimizacao(
                List<PosicaoPatio> posicoes,
                List<EquipamentoPatio> equipamentos,
                List<ConteinerPatio> inventario
        ) {
            this.posicoes = posicoes.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator
                            .comparing(PosicaoPatio::getLinha, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(PosicaoPatio::getColuna, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(posicao -> nivel(posicao.getCamadaOperacional()))
                            .thenComparing(PosicaoPatio::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            this.equipamentosOperacionais = equipamentos.stream()
                    .filter(Objects::nonNull)
                    .filter(equipamento -> equipamento.getStatusOperacional() == StatusEquipamento.OPERACIONAL)
                    .filter(equipamento -> equipamento.getLinha() != null && equipamento.getColuna() != null)
                    .toList();
            inventario.stream()
                    .filter(Objects::nonNull)
                    .filter(OtimizadorPesquisaOperacionalPatioServico.this::ocupaPosicaoAtiva)
                    .filter(conteiner -> conteiner.getPosicao().getId() != null)
                    .forEach(conteiner -> {
                        posicoesOcupadas.add(conteiner.getPosicao().getId());
                        ocupantePorPosicao.put(conteiner.getPosicao().getId(), conteiner);
                    });
        }

        private List<PosicaoPatio> proximasPosicoesLivres() {
            Map<ChavePilha, List<PosicaoPatio>> porPilha = posicoes.stream()
                    .filter(posicao -> posicao.getLinha() != null && posicao.getColuna() != null)
                    .collect(Collectors.groupingBy(
                            posicao -> new ChavePilha(posicao.getLinha(), posicao.getColuna()),
                            LinkedHashMap::new,
                            Collectors.toCollection(ArrayList::new)));
            List<PosicaoPatio> resultado = new ArrayList<>();
            for (List<PosicaoPatio> pilha : porPilha.values()) {
                pilha.sort(Comparator
                        .comparingInt((PosicaoPatio posicao) -> nivel(posicao.getCamadaOperacional()))
                        .thenComparing(PosicaoPatio::getId, Comparator.nullsLast(Comparator.naturalOrder())));
                PosicaoPatio proxima = proximaPosicaoDaPilha(pilha);
                if (proxima != null) {
                    resultado.add(proxima);
                }
            }
            resultado.sort(Comparator
                    .comparing(PosicaoPatio::getLinha)
                    .thenComparing(PosicaoPatio::getColuna)
                    .thenComparing(posicao -> nivel(posicao.getCamadaOperacional()))
                    .thenComparing(PosicaoPatio::getId));
            return resultado;
        }

        private PosicaoPatio proximaPosicaoDaPilha(List<PosicaoPatio> pilha) {
            for (PosicaoPatio posicao : pilha) {
                if (posicao.getId() == null || posicoesOcupadas.contains(posicao.getId())) {
                    continue;
                }
                int camada = nivel(posicao.getCamadaOperacional());
                if (camada <= 0) {
                    return null;
                }
                if (camada == 1 || possuiApoioOcupado(pilha, camada - 1)) {
                    return posicao;
                }
                return null;
            }
            return null;
        }

        private boolean possuiApoioOcupado(List<PosicaoPatio> pilha, int camadaApoio) {
            return pilha.stream()
                    .filter(posicao -> nivel(posicao.getCamadaOperacional()) == camadaApoio)
                    .map(PosicaoPatio::getId)
                    .filter(Objects::nonNull)
                    .anyMatch(posicoesOcupadas::contains);
        }

        private boolean possuiCoberturaReefer(PosicaoPatio posicao) {
            return equipamentosOperacionais.stream()
                    .anyMatch(equipamento -> Objects.equals(equipamento.getLinha(), posicao.getLinha())
                            && Objects.equals(equipamento.getColuna(), posicao.getColuna()));
        }

        private boolean possuiIsolamentoPerigoso(PosicaoPatio posicao) {
            return ocupantePorPosicao.values().stream()
                    .filter(OtimizadorPesquisaOperacionalPatioServico.this::ehCargaPerigosa)
                    .map(ConteinerPatio::getPosicao)
                    .filter(Objects::nonNull)
                    .noneMatch(ocupada -> Math.abs(ocupada.getLinha() - posicao.getLinha()) <= 1
                            && Math.abs(ocupada.getColuna() - posicao.getColuna()) <= 1);
        }

        private int distanciaEquipamento(PosicaoPatio posicao) {
            return equipamentosOperacionais.stream()
                    .mapToInt(equipamento -> Math.abs(equipamento.getLinha() - posicao.getLinha())
                            + Math.abs(equipamento.getColuna() - posicao.getColuna()))
                    .min()
                    .orElse(100);
        }

        private boolean pilhaVazia(PosicaoPatio posicao) {
            return ocupantesDaPilha(posicao).isEmpty();
        }

        private Collection<ConteinerPatio> ocupantesDaPilha(PosicaoPatio posicao) {
            return ocupantePorPosicao.values().stream()
                    .filter(ocupante -> ocupante.getPosicao() != null)
                    .filter(ocupante -> Objects.equals(ocupante.getPosicao().getLinha(), posicao.getLinha()))
                    .filter(ocupante -> Objects.equals(ocupante.getPosicao().getColuna(), posicao.getColuna()))
                    .toList();
        }

        private int ocupacaoBloco(String bloco) {
            String blocoNormalizado = normalizar(bloco);
            if (blocoNormalizado.isEmpty()) {
                return 0;
            }
            return (int) ocupantePorPosicao.values().stream()
                    .map(ConteinerPatio::getPosicao)
                    .filter(Objects::nonNull)
                    .filter(posicao -> Objects.equals(blocoNormalizado, normalizar(posicao.getBloco())))
                    .count();
        }

        private void registrar(ConteinerPatio conteiner, PosicaoPatio posicao) {
            posicoesOcupadas.add(posicao.getId());
            ocupantePorPosicao.put(posicao.getId(), conteiner);
            conteiner.setPosicao(posicao);
        }
    }

    private boolean ocupaPosicaoAtiva(ConteinerPatio conteiner) {
        return conteiner.getPosicao() != null
                && conteiner.getStatus() != StatusConteiner.LIBERADO
                && conteiner.getStatus() != StatusConteiner.DESPACHADO;
    }

    private record ChavePilha(Integer linha, Integer coluna) {
    }
}
