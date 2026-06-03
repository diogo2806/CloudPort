package br.com.cloudport.servicoyard.edi.parser;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Parser compartilhado para mensagens COPRAR e COARRI (UN/EDIFACT D.95B).
 *
 * COPRAR (Container Pre-Advice): plano de carga/descarga com posições previstas.
 *   Código de função no EQD: 1=carregar, 2=descarregar, 3=transbordo
 *
 * COARRI (Container Arrival/Departure Ack): confirmação das operações com tempo real.
 *   DTM+334 = hora real da operação
 *   FTX = texto livre
 *
 * Ambos os formatos compartilham a mesma estrutura de segmentos EQD/LOC/MEA/DTM.
 */
@Component
public class CoprarCoarriParser {

    private static final String SEG_SEP = "'";
    private static final String EL_SEP = "\\+";
    private static final DateTimeFormatter FMT_EDIFACT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public static class ResultadoParse {
        private String codigoNavio;
        private String codigoViagem;
        private final List<BayPlanContainer> containers = new ArrayList<>();

        public String getCodigoNavio() { return codigoNavio; }
        public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
        public String getCodigoViagem() { return codigoViagem; }
        public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
        public List<BayPlanContainer> getContainers() { return containers; }
    }

    public ResultadoParse parse(String edifact) {
        if (edifact == null || edifact.isBlank()) {
            throw new IllegalArgumentException("Conteúdo EDIFACT vazio");
        }

        String[] segmentos = edifact.split(SEG_SEP);
        ResultadoParse resultado = new ResultadoParse();
        BayPlanContainer containerAtual = null;

        for (String seg : segmentos) {
            String segLimpo = seg.strip();
            if (segLimpo.isEmpty()) continue;

            String[] campos = segLimpo.split(EL_SEP, -1);
            String tipo = campos[0];

            switch (tipo) {
                case "TDT" -> processarTdt(resultado, campos);
                case "EQD" -> {
                    containerAtual = processarEqd(campos);
                    resultado.getContainers().add(containerAtual);
                }
                case "LOC" -> {
                    String qual = campos.length > 1 ? campos[1] : "";
                    if ("147".equals(qual) && containerAtual != null) {
                        containerAtual.setPosicaoBay(
                            PosicaoBay.deCodigoEdifact(extrairCodigoEstiva(campos)));
                    } else if ("9".equals(qual) && containerAtual != null) {
                        containerAtual.setPortoDescarga(extrairCodigoPorto(campos));
                    }
                }
                case "DTM" -> {
                    // DTM+334 = hora real da operação (usado no COARRI)
                    if (containerAtual != null && campos.length > 1 && campos[1].startsWith("334:")) {
                        containerAtual.setHorarioOperacao(parsearDataEdifact(campos[1].substring(4)));
                        containerAtual.setStatusOperacao("CONCLUIDO");
                    }
                }
                case "MEA" -> {
                    if (containerAtual != null && campos.length > 1 && "WT".equals(campos[1]))
                        containerAtual.setPesoKg(extrairPeso(campos));
                }
                case "FTX" -> {
                    // Texto livre — ignorado, mas pode ser logado
                }
            }
        }

        return resultado;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void processarTdt(ResultadoParse resultado, String[] campos) {
        if (campos.length > 2 && !campos[2].isBlank()) {
            resultado.setCodigoViagem(campos[2]);
        }
        if (campos.length > 4) {
            String[] partes = campos[4].split(":", -1);
            if (partes.length > 1) {
                String nome = partes[partes.length - 1].strip();
                if (!nome.isBlank() && resultado.getCodigoNavio() == null)
                    resultado.setCodigoNavio(nome);
            }
        }
    }

    private BayPlanContainer processarEqd(String[] campos) {
        // EQD+CN+{codigo}+{isoCode}:...+...+...+...+{funcao}
        // funcao: 1=carregamento, 2=descarga, 3=transbordo
        BayPlanContainer c = new BayPlanContainer();
        c.setStatusOperacao("PLANEJADO");

        if (campos.length > 2) c.setCodigoContainer(campos[2].strip().toUpperCase());
        if (campos.length > 3) {
            String[] iso = campos[3].split(":", -1);
            c.setIsoCode(iso[0].strip());
        }

        // Último campo com dígito numérico é a função da operação
        int funcao = 2; // default: descarga
        for (int i = campos.length - 1; i >= 4; i--) {
            String f = campos[i].strip();
            if (f.equals("1") || f.equals("2") || f.equals("3")) {
                funcao = Integer.parseInt(f);
                break;
            }
        }
        c.setTipoOperacao(switch (funcao) {
            case 1 -> TipoOperacaoBayPlan.CARREGAMENTO;
            case 3 -> TipoOperacaoBayPlan.TRANSBORDO;
            default -> TipoOperacaoBayPlan.DESCARGA;
        });

        return c;
    }

    private String extrairCodigoPorto(String[] campos) {
        if (campos.length <= 2) return null;
        return campos[2].split(":", -1)[0].strip().toUpperCase();
    }

    private String extrairCodigoEstiva(String[] campos) {
        if (campos.length <= 2) return "000000";
        return campos[2].split(":", -1)[0].strip();
    }

    private Double extrairPeso(String[] campos) {
        if (campos.length <= 3) return null;
        try {
            return Double.parseDouble(campos[3].split(":", -1)[0].strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parsearDataEdifact(String valor) {
        // Formato: yyyyMMddHHmm:203
        String[] partes = valor.split(":", -1);
        try {
            return LocalDateTime.parse(partes[0].strip(), FMT_EDIFACT);
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }
}
