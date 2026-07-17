package br.com.cloudport.servicoyard.edi.parser;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BaplieParser {

    private static final Set<String> RELEASES_SUPORTADOS = Set.of("95B", "13B");
    private static final Set<String> QUALIFICADORES_PESO_BRUTO = Set.of("WT", "G", "AET");
    private static final Set<String> QUALIFICADORES_VGM = Set.of("VGM");
    private static final Set<String> UNIDADES_SUPORTADAS = Set.of(
            "KGM", "KG", "KGS", "TNE", "TON", "T", "LBR", "LB", "LBS", "GRM", "G",
            "CEL", "C", "FAH", "F", "KEL", "K", "CMT", "CM", "MTR", "M", "MMT", "MM");

    public BayPlan parse(String edifact) {
        if (edifact == null || edifact.isBlank()) {
            throw new IllegalArgumentException("Conteúdo BAPLIE vazio.");
        }

        BayPlan bayPlan = new BayPlan();
        bayPlan.setStatus(StatusBayPlan.ATIVO);
        bayPlan.setOrigemMensagem("BAPLIE");

        List<BayPlanContainer> containers = new ArrayList<>();
        BayPlanContainer containerAtual = null;
        StringBuilder segmentosAtuais = null;
        PosicaoBay posicaoPendente = null;
        String segmentoPosicaoPendente = null;
        TipoOperacaoBayPlan operacaoPadrao = null;
        boolean perfilValidado = false;

        for (String segmentoOriginal : edifact.replace("\r", "").split("'")) {
            String segmento = segmentoOriginal.trim();
            if (segmento.isEmpty()) {
                continue;
            }

            String[] campos = segmento.split("\\+", -1);
            String tipoSegmento = campos[0].trim().toUpperCase(Locale.ROOT);

            if ("UNH".equals(tipoSegmento)) {
                validarPerfilUnh(campos);
                perfilValidado = true;
                continue;
            }
            if ("BGM".equals(tipoSegmento)) {
                operacaoPadrao = extrairOperacao(campos);
                continue;
            }
            if ("TDT".equals(tipoSegmento)) {
                processarTdt(campos, bayPlan);
                continue;
            }

            if ("LOC".equals(tipoSegmento) && campos.length > 2) {
                String qualificador = primeiroComponente(campos[1]);
                String valor = primeiroComponente(campos[2]);

                if ("147".equals(qualificador)) {
                    if (containerAtual != null && containerAtual.getPosicaoBay() != null) {
                        finalizarContainer(containerAtual, segmentosAtuais, operacaoPadrao, bayPlan, containers);
                        containerAtual = null;
                        segmentosAtuais = null;
                    }
                    PosicaoBay posicao = PosicaoBay.deCodigoEdifact(valor);
                    if (containerAtual != null) {
                        containerAtual.setPosicaoBay(posicao);
                        appendSegmento(segmentosAtuais, segmento);
                    } else {
                        posicaoPendente = posicao;
                        segmentoPosicaoPendente = segmento;
                    }
                    continue;
                }

                if (containerAtual != null) {
                    appendSegmento(segmentosAtuais, segmento);
                }
                processarLoc(qualificador, valor, bayPlan, containerAtual);
                continue;
            }

            if ("EQD".equals(tipoSegmento)) {
                if (containerAtual != null) {
                    finalizarContainer(containerAtual, segmentosAtuais, operacaoPadrao, bayPlan, containers);
                }
                containerAtual = processarEqd(campos);
                segmentosAtuais = new StringBuilder();
                if (segmentoPosicaoPendente != null) {
                    appendSegmento(segmentosAtuais, segmentoPosicaoPendente);
                }
                appendSegmento(segmentosAtuais, segmento);
                containerAtual.setPosicaoBay(posicaoPendente);
                posicaoPendente = null;
                segmentoPosicaoPendente = null;
                continue;
            }

            if (containerAtual == null) {
                continue;
            }

            appendSegmento(segmentosAtuais, segmento);
            switch (tipoSegmento) {
                case "MEA" -> processarMea(campos, containerAtual);
                case "RFF" -> processarRff(campos, containerAtual, bayPlan);
                case "TMP" -> processarTmp(campos, containerAtual);
                case "RNG" -> processarRng(campos, containerAtual);
                case "DGS" -> processarDgs(campos, containerAtual);
                case "HAN" -> processarHan(campos, containerAtual);
                case "DIM" -> processarDim(campos, containerAtual);
                case "SGP", "ATT", "FTX" -> processarSegregacao(campos, containerAtual);
                default -> {
                    // Segmento preservado em segmentosOriginais para auditoria.
                }
            }
        }

        if (containerAtual != null) {
            finalizarContainer(containerAtual, segmentosAtuais, operacaoPadrao, bayPlan, containers);
        }
        if (!perfilValidado) {
            throw new IllegalArgumentException("BAPLIE sem perfil UNH válido.");
        }
        validarIdentidade(bayPlan);
        if (containers.isEmpty()) {
            throw new IllegalArgumentException("BAPLIE sem equipamentos válidos.");
        }

        containers.forEach(bayPlan::adicionarContainer);
        return bayPlan;
    }

    private void validarPerfilUnh(String[] campos) {
        if (campos.length < 3 || campos[2].isBlank()) {
            throw new IllegalArgumentException("UNH do BAPLIE não informa o perfil da mensagem.");
        }

        String[] perfil = campos[2].split(":", -1);
        if (perfil.length < 4
                || !"BAPLIE".equalsIgnoreCase(perfil[0])
                || !"D".equalsIgnoreCase(perfil[1])
                || !RELEASES_SUPORTADOS.contains(perfil[2].toUpperCase(Locale.ROOT))
                || !"UN".equalsIgnoreCase(perfil[3])) {
            throw new IllegalArgumentException("Perfil BAPLIE não suportado: " + campos[2] + ".");
        }

        String release = perfil[2].toUpperCase(Locale.ROOT);
        String associacao = perfil.length > 4 ? perfil[4].toUpperCase(Locale.ROOT) : "";
        if ("13B".equals(release) && !associacao.startsWith("SMDG31")) {
            throw new IllegalArgumentException("BAPLIE D.13B exige o perfil SMDG31.");
        }
        if ("95B".equals(release) && !associacao.isBlank() && !associacao.startsWith("SMDG")) {
            throw new IllegalArgumentException("Associação BAPLIE D.95B não suportada: " + associacao + ".");
        }
    }

    private TipoOperacaoBayPlan extrairOperacao(String[] campos) {
        String conteudo = String.join(":", campos).toUpperCase(Locale.ROOT);
        if (conteudo.contains("LOADONLY") || conteudo.contains("LOAD_ONLY")) {
            return TipoOperacaoBayPlan.CARREGAMENTO;
        }
        if (conteudo.contains("DISCHONLY") || conteudo.contains("DISCHARGE")) {
            return TipoOperacaoBayPlan.DESCARGA;
        }
        if (conteudo.contains("RESTOW") || conteudo.contains("SHIFT")) {
            return TipoOperacaoBayPlan.REMANEJAMENTO;
        }
        if (conteudo.contains("TRANSSHIP") || conteudo.contains("TRANSIT")) {
            return TipoOperacaoBayPlan.TRANSBORDO;
        }
        return null;
    }

    private void processarTdt(String[] campos, BayPlan bayPlan) {
        if (campos.length > 2 && !campos[2].isBlank()) {
            bayPlan.setCodigoViagem(primeiroComponente(campos[2]));
        }

        String identificacaoNavio = null;
        if (campos.length > 8 && !campos[8].isBlank()) {
            identificacaoNavio = campos[8];
        } else if (campos.length > 4 && !campos[4].isBlank()) {
            identificacaoNavio = campos[4];
        }
        if (identificacaoNavio == null) {
            return;
        }

        List<String> componentes = Arrays.stream(identificacaoNavio.split(":", -1))
                .map(String::trim)
                .filter(valor -> !valor.isBlank())
                .toList();
        if (componentes.isEmpty()) {
            return;
        }

        String primeiro = componentes.get(0);
        String nome = componentes.get(componentes.size() - 1);
        String codigo = null;
        if (primeiro.matches("\\d{7}")) {
            codigo = primeiro;
        } else if (primeiro.length() >= 3 && !primeiro.matches("\\d+")) {
            codigo = primeiro;
        }
        if (codigo == null && !nome.isBlank()) {
            codigo = nome.toUpperCase(Locale.ROOT);
        }

        bayPlan.setCodigoNavio(codigo);
        bayPlan.setNomeNavio(nome);
    }

    private BayPlanContainer processarEqd(String[] campos) {
        if (campos.length < 4 || primeiroComponente(campos[2]).isBlank()) {
            throw new IllegalArgumentException("EQD sem identificação do equipamento.");
        }

        BayPlanContainer container = new BayPlanContainer();
        container.setCodigoContainer(primeiroComponente(campos[2]).toUpperCase(Locale.ROOT));
        container.setIsoCode(primeiroComponente(campos[3]).toUpperCase(Locale.ROOT));
        container.setStatusOperacao("PLANEJADO");
        container.setEstadoCarga(extrairEstadoCarga(campos));
        return container;
    }

    private EstadoCargaContainer extrairEstadoCarga(String[] campos) {
        for (int indice = campos.length - 1; indice >= 4; indice--) {
            String indicador = primeiroComponente(campos[indice]);
            if ("5".equals(indicador)) {
                return EstadoCargaContainer.CHEIO;
            }
            if ("4".equals(indicador)) {
                return EstadoCargaContainer.VAZIO;
            }
        }
        return EstadoCargaContainer.DESCONHECIDO;
    }

    private void processarLoc(String qualificador,
                              String valor,
                              BayPlan bayPlan,
                              BayPlanContainer container) {
        switch (qualificador) {
            case "5" -> bayPlan.setPortoCarga(valor);
            case "61" -> bayPlan.setPortoDescarga(valor);
            case "9" -> {
                if (container != null) {
                    container.setPortoCarga(valor);
                }
            }
            case "11" -> {
                if (container != null) {
                    container.setPortoDescarga(valor);
                }
            }
            default -> {
                // Qualificador fora do escopo persistido, mas mantido no conteúdo original.
            }
        }
    }

    private void processarMea(String[] campos, BayPlanContainer container) {
        String qualificador = localizarQualificador(campos, QUALIFICADORES_VGM);
        boolean vgm = qualificador != null;
        if (!vgm) {
            qualificador = localizarQualificador(campos, QUALIFICADORES_PESO_BRUTO);
        }
        if (qualificador == null) {
            return;
        }

        Medida medida = extrairMedida(campos);
        double pesoKg = converterPesoParaKg(medida.valor(), medida.unidade());
        if (!Double.isFinite(pesoKg) || pesoKg <= 0) {
            throw new IllegalArgumentException("Peso BAPLIE inválido para " + container.getCodigoContainer() + ".");
        }

        if (vgm) {
            container.setPesoVgmKg(pesoKg);
            container.setUnidadeVgmOriginal(medida.unidade());
            container.setOrigemVgm("BAPLIE");
            container.setStatusVgm("VERIFICADO");
        } else {
            container.setPesoKg(pesoKg);
            container.setUnidadePesoOriginal(medida.unidade());
        }
    }

    private void processarRff(String[] campos, BayPlanContainer container, BayPlan bayPlan) {
        if (campos.length < 2) {
            return;
        }
        String qualificador = primeiroComponente(campos[1]).toUpperCase(Locale.ROOT);
        String valor = ultimoComponente(campos[1]);
        if ("BM".equals(qualificador) || "BL".equals(qualificador)) {
            container.setReferenciaBl(valor);
        }
        if ("VON".equals(qualificador)
                && (bayPlan.getCodigoViagem() == null || bayPlan.getCodigoViagem().isBlank())) {
            bayPlan.setCodigoViagem(valor);
        }
    }

    private void processarTmp(String[] campos, BayPlanContainer container) {
        Medida medida = extrairMedida(campos);
        container.setTemperaturaRequeridaC(
                converterTemperaturaParaCelsius(medida.valor(), medida.unidade()));
        container.setReefer(true);
    }

    private void processarRng(String[] campos, BayPlanContainer container) {
        List<Double> valores = extrairNumeros(campos);
        if (valores.size() < 2) {
            throw new IllegalArgumentException("RNG sem faixa de temperatura válida para "
                    + container.getCodigoContainer() + ".");
        }
        String unidade = localizarUnidade(campos);
        double primeiro = converterTemperaturaParaCelsius(valores.get(0), unidade);
        double segundo = converterTemperaturaParaCelsius(valores.get(1), unidade);
        container.setTemperaturaMinimaC(Math.min(primeiro, segundo));
        container.setTemperaturaMaximaC(Math.max(primeiro, segundo));
        container.setReefer(true);
    }

    private void processarDgs(String[] campos, BayPlanContainer container) {
        container.setPerigoso(true);
        if (campos.length > 2) {
            container.setClasseImo(anexarDistinto(container.getClasseImo(), primeiroComponente(campos[2])));
        }
        if (campos.length > 3) {
            container.setNumeroOnu(anexarDistinto(container.getNumeroOnu(), primeiroComponente(campos[3])));
        }
        if (campos.length > 5) {
            container.setGrupoEmbalagem(
                    anexarDistinto(container.getGrupoEmbalagem(), primeiroComponente(campos[5])));
        }
        if (campos.length > 6) {
            container.setCodigoEmergencia(anexarDistinto(container.getCodigoEmergencia(), campos[6]));
        }
    }

    private void processarHan(String[] campos, BayPlanContainer container) {
        String instrucao = juntarCampos(campos, 1);
        container.setInstrucaoManuseio(anexarDistinto(container.getInstrucaoManuseio(), instrucao));

        String normalizado = instrucao.toUpperCase(Locale.ROOT);
        if (normalizado.contains("REEFER") || normalizado.matches(".*(^|:)RF(:|$).*$")) {
            container.setReefer(true);
        }
        if (normalizado.contains("OOG") || normalizado.contains("OUT OF GAUGE")) {
            container.setOog(true);
        }
    }

    private void processarDim(String[] campos, BayPlanContainer container) {
        if (campos.length < 3) {
            return;
        }
        String qualificador = primeiroComponente(campos[1]).toUpperCase(Locale.ROOT);
        Medida medida = extrairMedida(campos);
        double valorCm = converterComprimentoParaCm(medida.valor(), medida.unidade());
        if (!Double.isFinite(valorCm) || valorCm <= 0) {
            return;
        }

        switch (qualificador) {
            case "5", "FRONT", "OF" -> container.setExcessoFrontalCm(valorCm);
            case "6", "REAR", "OB" -> container.setExcessoTraseiroCm(valorCm);
            case "7", "LEFT", "OL" -> container.setExcessoEsquerdoCm(valorCm);
            case "8", "RIGHT", "OR" -> container.setExcessoDireitoCm(valorCm);
            case "9", "HEIGHT", "OH" -> container.setExcessoAlturaCm(valorCm);
            default -> {
                return;
            }
        }
        container.setOog(true);
    }

    private void processarSegregacao(String[] campos, BayPlanContainer container) {
        String segmento = String.join(":", campos).toUpperCase(Locale.ROOT);
        if (!segmento.contains("SEG") && !"SGP".equalsIgnoreCase(campos[0])) {
            return;
        }
        String valor = juntarCampos(campos, Math.min(2, campos.length));
        if (!valor.isBlank()) {
            container.setGrupoSegregacao(anexarDistinto(container.getGrupoSegregacao(), valor));
        }
    }

    private void finalizarContainer(BayPlanContainer container,
                                     StringBuilder segmentos,
                                     TipoOperacaoBayPlan operacaoPadrao,
                                     BayPlan bayPlan,
                                     List<BayPlanContainer> containers) {
        if (container.getTipoOperacao() == null) {
            container.setTipoOperacao(operacaoPadrao != null
                    ? operacaoPadrao
                    : inferirOperacao(container, bayPlan));
        }
        container.setSegmentosOriginais(segmentos != null ? segmentos.toString() : null);
        validarContainer(container);
        containers.add(container);
    }

    private TipoOperacaoBayPlan inferirOperacao(BayPlanContainer container, BayPlan bayPlan) {
        if (iguaisIgnorandoCaixa(container.getPortoCarga(), bayPlan.getPortoCarga())) {
            return TipoOperacaoBayPlan.CARREGAMENTO;
        }
        if (iguaisIgnorandoCaixa(container.getPortoDescarga(), bayPlan.getPortoDescarga())
                || iguaisIgnorandoCaixa(container.getPortoDescarga(), bayPlan.getPortoCarga())) {
            return TipoOperacaoBayPlan.DESCARGA;
        }
        return TipoOperacaoBayPlan.TRANSBORDO;
    }

    private void validarIdentidade(BayPlan bayPlan) {
        if (bayPlan.getCodigoNavio() == null || bayPlan.getCodigoNavio().isBlank()) {
            throw new IllegalArgumentException("BAPLIE sem identificação real do navio no TDT.");
        }
        if (bayPlan.getCodigoViagem() == null || bayPlan.getCodigoViagem().isBlank()) {
            throw new IllegalArgumentException("BAPLIE sem identificação da viagem no TDT/RFF.");
        }
    }

    private void validarContainer(BayPlanContainer container) {
        if (container.getCodigoContainer() == null || container.getCodigoContainer().isBlank()) {
            throw new IllegalArgumentException("BAPLIE contém equipamento sem identificação.");
        }
        if (container.getIsoCode() == null || container.getIsoCode().isBlank()) {
            throw new IllegalArgumentException("BAPLIE sem ISO code para " + container.getCodigoContainer() + ".");
        }
        PosicaoBay posicao = container.getPosicaoBay();
        if (posicao == null || posicao.getBay() == null || posicao.getBay() <= 0
                || posicao.getRow() == null || posicao.getRow() < 0
                || posicao.getTier() == null || posicao.getTier() <= 0) {
            throw new IllegalArgumentException("BAPLIE sem posição válida para " + container.getCodigoContainer() + ".");
        }
        if (container.getEstadoCarga() == null
                || container.getEstadoCarga() == EstadoCargaContainer.DESCONHECIDO) {
            throw new IllegalArgumentException("BAPLIE sem indicador cheio/vazio para "
                    + container.getCodigoContainer() + ".");
        }
        if (container.getTipoOperacao() == null) {
            throw new IllegalArgumentException("BAPLIE sem operação para " + container.getCodigoContainer() + ".");
        }
        Double peso = container.getPesoOperacionalKg();
        if (peso == null || !Double.isFinite(peso) || peso <= 0) {
            throw new IllegalArgumentException("BAPLIE sem peso operacional válido para "
                    + container.getCodigoContainer() + ".");
        }
        if (container.isPerigoso()
                && (container.getClasseImo() == null || container.getClasseImo().isBlank()
                || container.getNumeroOnu() == null || container.getNumeroOnu().isBlank())) {
            throw new IllegalArgumentException("Carga perigosa sem classe IMO ou número ONU para "
                    + container.getCodigoContainer() + ".");
        }
    }

    private String localizarQualificador(String[] campos, Set<String> qualificadores) {
        for (int indice = 1; indice < Math.min(campos.length, 3); indice++) {
            String valor = primeiroComponente(campos[indice]).toUpperCase(Locale.ROOT);
            if (qualificadores.contains(valor)) {
                return valor;
            }
        }
        return null;
    }

    private Medida extrairMedida(String[] campos) {
        Double valor = null;
        String unidade = null;
        for (int indice = 2; indice < campos.length; indice++) {
            for (String componente : campos[indice].split(":", -1)) {
                String limpo = componente.trim();
                if (limpo.isBlank()) {
                    continue;
                }
                if (valor == null && ehNumero(limpo)) {
                    valor = Double.parseDouble(limpo.replace(',', '.'));
                } else if (unidade == null && ehUnidade(limpo)) {
                    unidade = limpo.toUpperCase(Locale.ROOT);
                }
            }
        }
        if (valor == null || unidade == null) {
            throw new IllegalArgumentException("Segmento de medida sem valor e unidade suportados: "
                    + String.join("+", campos) + ".");
        }
        return new Medida(valor, unidade);
    }

    private List<Double> extrairNumeros(String[] campos) {
        List<Double> valores = new ArrayList<>();
        for (int indice = 2; indice < campos.length; indice++) {
            for (String componente : campos[indice].split(":", -1)) {
                String limpo = componente.trim();
                if (ehNumero(limpo)) {
                    valores.add(Double.parseDouble(limpo.replace(',', '.')));
                }
            }
        }
        return valores;
    }

    private String localizarUnidade(String[] campos) {
        for (int indice = 2; indice < campos.length; indice++) {
            for (String componente : campos[indice].split(":", -1)) {
                String limpo = componente.trim();
                if (ehUnidade(limpo)) {
                    return limpo.toUpperCase(Locale.ROOT);
                }
            }
        }
        throw new IllegalArgumentException("Unidade não informada no segmento "
                + String.join("+", campos) + ".");
    }

    private double converterPesoParaKg(double valor, String unidade) {
        return switch (unidade.toUpperCase(Locale.ROOT)) {
            case "KGM", "KG", "KGS" -> valor;
            case "TNE", "TON", "T" -> valor * 1000.0;
            case "LBR", "LB", "LBS" -> valor * 0.45359237;
            case "GRM", "G" -> valor / 1000.0;
            default -> throw new IllegalArgumentException(
                    "Unidade de peso BAPLIE não suportada: " + unidade + ".");
        };
    }

    private double converterTemperaturaParaCelsius(double valor, String unidade) {
        return switch (unidade.toUpperCase(Locale.ROOT)) {
            case "CEL", "C" -> valor;
            case "FAH", "F" -> (valor - 32.0) * 5.0 / 9.0;
            case "KEL", "K" -> valor - 273.15;
            default -> throw new IllegalArgumentException(
                    "Unidade de temperatura não suportada: " + unidade + ".");
        };
    }

    private double converterComprimentoParaCm(double valor, String unidade) {
        return switch (unidade.toUpperCase(Locale.ROOT)) {
            case "CMT", "CM" -> valor;
            case "MTR", "M" -> valor * 100.0;
            case "MMT", "MM" -> valor / 10.0;
            default -> throw new IllegalArgumentException(
                    "Unidade de dimensão OOG não suportada: " + unidade + ".");
        };
    }

    private boolean ehUnidade(String valor) {
        return UNIDADES_SUPORTADAS.contains(valor.toUpperCase(Locale.ROOT));
    }

    private boolean ehNumero(String valor) {
        return valor != null && valor.trim().replace(',', '.').matches("[-+]?\\d+(\\.\\d+)?");
    }

    private String primeiroComponente(String valor) {
        if (valor == null) {
            return "";
        }
        String[] componentes = valor.split(":", -1);
        return componentes.length == 0 ? "" : componentes[0].trim();
    }

    private String ultimoComponente(String valor) {
        if (valor == null) {
            return "";
        }
        String[] componentes = valor.split(":", -1);
        for (int indice = componentes.length - 1; indice >= 0; indice--) {
            if (!componentes[indice].isBlank()) {
                return componentes[indice].trim();
            }
        }
        return "";
    }

    private String juntarCampos(String[] campos, int inicio) {
        if (inicio >= campos.length) {
            return "";
        }
        return Arrays.stream(campos, inicio, campos.length)
                .map(String::trim)
                .filter(valor -> !valor.isBlank())
                .reduce((esquerda, direita) -> esquerda + ":" + direita)
                .orElse("");
    }

    private String anexarDistinto(String atual, String novoValor) {
        if (novoValor == null || novoValor.isBlank()) {
            return atual;
        }
        String limpo = novoValor.trim();
        if (atual == null || atual.isBlank()) {
            return limpo;
        }
        return Arrays.stream(atual.split(","))
                .map(String::trim)
                .anyMatch(valor -> valor.equalsIgnoreCase(limpo))
                ? atual
                : atual + "," + limpo;
    }

    private boolean iguaisIgnorandoCaixa(String primeiro, String segundo) {
        return primeiro != null && segundo != null && primeiro.equalsIgnoreCase(segundo);
    }

    private void appendSegmento(StringBuilder builder, String segmento) {
        if (builder != null) {
            builder.append(segmento).append("'\n");
        }
    }

    private record Medida(double valor, String unidade) {
    }
}
