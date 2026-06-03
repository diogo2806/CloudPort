package br.com.cloudport.servicoyard.edi.parser;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Parser de mensagens BAPLIE (UN/EDIFACT D.95B).
 * BAPLIE descreve o plano de estiva do navio: posição bay/row/tier de cada contêiner.
 *
 * Segmentos tratados:
 *   TDT  – dados do transporte (navio, viagem)
 *   LOC+5  – porto de origem
 *   LOC+61 – porto de destino
 *   EQD  – dados do equipamento (código, ISO type)
 *   LOC+147 – posição de estiva (bay/row/tier)
 *   LOC+9  – porto de descarga do contêiner
 *   LOC+11 – porto de carga do contêiner
 *   MEA+WT – peso bruto
 *   RFF+BL – referência Bill of Lading
 *   HAN    – instrução de manuseio (carga perigosa, refrigerado, etc.)
 */
@Component
public class BaplieParser {

    private static final String SEG_SEP = "'";
    private static final String EL_SEP = "\\+";

    public BayPlan parse(String edifact) {
        if (edifact == null || edifact.isBlank()) {
            throw new IllegalArgumentException("Conteúdo EDIFACT vazio");
        }

        String[] segmentos = edifact.split(SEG_SEP);
        BayPlan bayPlan = new BayPlan();
        bayPlan.setStatus(StatusBayPlan.RASCUNHO);
        bayPlan.setOrigemMensagem("BAPLIE");

        List<BayPlanContainer> containers = new ArrayList<>();
        BayPlanContainer containerAtual = null;

        for (String seg : segmentos) {
            String segLimpo = seg.strip();
            if (segLimpo.isEmpty()) continue;

            String[] campos = segLimpo.split(EL_SEP, -1);
            String tipo = campos[0];

            switch (tipo) {
                case "TDT" -> processarTdt(bayPlan, campos);
                case "LOC"  -> {
                    String qualificador = campos.length > 1 ? campos[1] : "";
                    switch (qualificador) {
                        case "5"   -> bayPlan.setPortoCarga(extrairCodigoPorto(campos));
                        case "61"  -> bayPlan.setPortoDescarga(extrairCodigoPorto(campos));
                        case "147" -> {
                            if (containerAtual != null)
                                containerAtual.setPosicaoBay(
                                    PosicaoBay.deCodigoEdifact(extrairCodigoEstiva(campos)));
                        }
                        case "9"   -> { if (containerAtual != null)
                                containerAtual.setPortoDescarga(extrairCodigoPorto(campos)); }
                        case "11"  -> { if (containerAtual != null)
                                containerAtual.setPortoCarga(extrairCodigoPorto(campos)); }
                    }
                }
                case "EQD" -> {
                    containerAtual = processarEqd(campos);
                    containers.add(containerAtual);
                }
                case "MEA" -> {
                    if (containerAtual != null && campos.length > 1 && "WT".equals(campos[1]))
                        containerAtual.setPesoKg(extrairPeso(campos));
                }
                case "RFF" -> {
                    if (containerAtual != null && campos.length > 1 && campos[1].startsWith("BL:"))
                        containerAtual.setReferenciaBl(campos[1].substring(3));
                }
                case "HAN" -> {
                    if (containerAtual != null && campos.length > 1 && !campos[1].isBlank())
                        containerAtual.setStatusOperacao("MANUSEIO:" + campos[1]);
                }
            }
        }

        containers.forEach(bayPlan::adicionarContainer);

        if (bayPlan.getCodigoNavio() == null || bayPlan.getCodigoViagem() == null) {
            throw new IllegalArgumentException(
                    "BAPLIE inválido: segmento TDT ausente ou incompleto");
        }

        return bayPlan;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void processarTdt(BayPlan bp, String[] campos) {
        // TDT+20+{viagem}++{lineId}:{nomNavio}
        if (campos.length > 2 && !campos[2].isBlank()) {
            bp.setCodigoViagem(campos[2]);
        }
        if (campos.length > 4) {
            String[] partes = campos[4].split(":", -1);
            if (partes.length > 1) {
                String nome = partes[partes.length - 1].strip();
                if (!nome.isBlank()) bp.setNomeNavio(nome);
                // Usa o nome do navio como código quando não há código explícito
                if (bp.getCodigoNavio() == null) bp.setCodigoNavio(nome);
            }
        }
        // Fallback para garantir codigoNavio não nulo
        if (bp.getCodigoNavio() == null && bp.getCodigoViagem() != null) {
            bp.setCodigoNavio("NAVIO_" + bp.getCodigoViagem());
        }
    }

    private BayPlanContainer processarEqd(String[] campos) {
        // EQD+CN+{codigoContainer}+{isoCode}:...
        BayPlanContainer c = new BayPlanContainer();
        c.setTipoOperacao(TipoOperacaoBayPlan.DESCARGA);
        c.setStatusOperacao("PLANEJADO");
        if (campos.length > 2) {
            c.setCodigoContainer(campos[2].strip().toUpperCase());
        }
        if (campos.length > 3) {
            String[] iso = campos[3].split(":", -1);
            c.setIsoCode(iso[0].strip());
        }
        return c;
    }

    private String extrairCodigoPorto(String[] campos) {
        // LOC+Q+{portCode}:{codelist}:{codetable}
        if (campos.length <= 2) return null;
        String[] partes = campos[2].split(":", -1);
        return partes[0].strip().toUpperCase();
    }

    private String extrairCodigoEstiva(String[] campos) {
        // LOC+147+{BBRRTT}:{codelist}:{codetable}
        if (campos.length <= 2) return "000000";
        return campos[2].split(":", -1)[0].strip();
    }

    private Double extrairPeso(String[] campos) {
        // MEA+WT++{peso}:{unidade}
        if (campos.length <= 3) return null;
        String[] partes = campos[3].split(":", -1);
        try {
            return Double.parseDouble(partes[0].strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
