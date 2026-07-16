package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.RelatorioOperacionalIntegradoDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RelatorioOperacionalExportacaoServico {

    private static final int LINHAS_POR_PAGINA = 48;

    private final IntegracaoNavioPatioServico integracaoServico;

    public RelatorioOperacionalExportacaoServico(IntegracaoNavioPatioServico integracaoServico) {
        this.integracaoServico = integracaoServico;
    }

    public byte[] gerarCsv(Long visitaId) {
        RelatorioOperacionalIntegradoDTO relatorio = integracaoServico.gerarRelatorioOperacionalIntegrado(visitaId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("visita;navio;fase;item;movimento;produto;tipo_carga;quantidade;peso_t;porao_planejado;porao_real;posicao_estiva_planejada;posicao_estiva_real;posicao_patio_planejada;posicao_patio_real;status_item;status_integracao\n");
        for (ItemOperacaoNavioDTO item : relatorio.itens()) {
            csv.append(campo(relatorio.visita().codigoVisita())).append(';')
                    .append(campo(relatorio.visita().navioNome())).append(';')
                    .append(campo(relatorio.visita().fase())).append(';')
                    .append(campo(item.codigoLote())).append(';')
                    .append(campo(item.tipoMovimento())).append(';')
                    .append(campo(item.produto())).append(';')
                    .append(campo(item.tipoCarga())).append(';')
                    .append(campo(item.quantidade())).append(';')
                    .append(campo(item.pesoTotalToneladas())).append(';')
                    .append(campo(item.poraoPlanejado())).append(';')
                    .append(campo(item.poraoReal())).append(';')
                    .append(campo(item.posicaoPlanejada())).append(';')
                    .append(campo(item.posicaoReal())).append(';')
                    .append(campo(item.posicaoPatioPlanejada())).append(';')
                    .append(campo(item.posicaoPatioReal())).append(';')
                    .append(campo(item.status())).append(';')
                    .append(campo(item.statusIntegracaoPatio())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] gerarPdf(Long visitaId) {
        RelatorioOperacionalIntegradoDTO relatorio = integracaoServico.gerarRelatorioOperacionalIntegrado(visitaId);
        List<String> linhas = montarLinhas(relatorio);
        return gerarPdfTexto(linhas);
    }

    private List<String> montarLinhas(RelatorioOperacionalIntegradoDTO relatorio) {
        List<String> linhas = new ArrayList<>();
        linhas.add("CLOUDPORT - RELATORIO OPERACIONAL INTEGRADO");
        linhas.add("Visita: " + texto(relatorio.visita().codigoVisita()));
        linhas.add("Navio: " + texto(relatorio.visita().navioNome()) + " | Fase: " + texto(relatorio.visita().fase()));
        linhas.add("Berco: " + texto(relatorio.visita().bercoAtual() == null ? relatorio.visita().bercoPrevisto() : relatorio.visita().bercoAtual()));
        linhas.add("Itens planejados: " + relatorio.resumoOperacional().totalItensPlanejados()
                + " | Operados: " + relatorio.resumoOperacional().totalItensOperados());
        linhas.add("Peso planejado: " + numero(relatorio.resumoOperacional().pesoPlanejado())
                + " t | Peso operado: " + numero(relatorio.resumoOperacional().pesoOperado()) + " t");
        linhas.add("Reservas: " + relatorio.reservasPatio().size()
                + " | Ordens: " + relatorio.ordensPatio().size()
                + " | Alertas: " + relatorio.divergenciasAlertas().size());
        linhas.add("");
        linhas.add("ITENS OPERACIONAIS");
        for (ItemOperacaoNavioDTO item : relatorio.itens()) {
            linhas.add(texto(item.codigoLote()) + " | " + item.tipoMovimento() + " | " + texto(item.produto())
                    + " | " + numero(item.pesoTotalToneladas()) + " t | " + item.status());
            linhas.add("  Estiva: " + texto(item.poraoPlanejado()) + "/" + texto(item.posicaoPlanejada())
                    + " -> " + texto(item.poraoReal()) + "/" + texto(item.posicaoReal()));
            linhas.add("  Patio: " + texto(item.posicaoPatioPlanejada()) + " -> " + texto(item.posicaoPatioReal())
                    + " | Integracao: " + texto(item.statusIntegracaoPatio()));
        }
        if (!relatorio.divergenciasAlertas().isEmpty()) {
            linhas.add("");
            linhas.add("ALERTAS E DIVERGENCIAS");
            relatorio.divergenciasAlertas().forEach(alerta -> linhas.add(
                    texto(alerta.severidade()) + " | " + texto(alerta.tipo()) + " | " + texto(alerta.mensagem())
            ));
        }
        return linhas;
    }

    private byte[] gerarPdfTexto(List<String> linhas) {
        try {
            List<List<String>> paginas = paginar(linhas);
            int totalObjetos = 3 + paginas.size() * 2;
            int objetoFonte = totalObjetos;
            List<byte[]> objetos = new ArrayList<>();
            objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));

            StringBuilder filhos = new StringBuilder();
            for (int indice = 0; indice < paginas.size(); indice++) {
                filhos.append(3 + indice * 2).append(" 0 R ");
            }
            objetos.add(bytes("<< /Type /Pages /Kids [" + filhos + "] /Count " + paginas.size() + " >>"));

            for (int indice = 0; indice < paginas.size(); indice++) {
                int objetoPagina = 3 + indice * 2;
                int objetoConteudo = objetoPagina + 1;
                objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 "
                        + objetoFonte + " 0 R >> >> /Contents " + objetoConteudo + " 0 R >>"));
                byte[] stream = bytes(conteudoPagina(paginas.get(indice)));
                ByteArrayOutputStream conteudo = new ByteArrayOutputStream();
                conteudo.write(bytes("<< /Length " + stream.length + " >>\nstream\n"));
                conteudo.write(stream);
                conteudo.write(bytes("\nendstream"));
                objetos.add(conteudo.toByteArray());
            }
            objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write(bytes("%PDF-1.4\n%CloudPort\n"));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int indice = 0; indice < objetos.size(); indice++) {
                offsets.add(pdf.size());
                pdf.write(bytes((indice + 1) + " 0 obj\n"));
                pdf.write(objetos.get(indice));
                pdf.write(bytes("\nendobj\n"));
            }
            int inicioXref = pdf.size();
            pdf.write(bytes("xref\n0 " + (objetos.size() + 1) + "\n"));
            pdf.write(bytes("0000000000 65535 f \n"));
            for (int indice = 1; indice < offsets.size(); indice++) {
                pdf.write(bytes(String.format("%010d 00000 n \n", offsets.get(indice))));
            }
            pdf.write(bytes("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                    + inicioXref + "\n%%EOF"));
            return pdf.toByteArray();
        } catch (IOException erro) {
            throw new IllegalStateException("Nao foi possivel gerar o PDF operacional.", erro);
        }
    }

    private List<List<String>> paginar(List<String> linhas) {
        List<List<String>> paginas = new ArrayList<>();
        for (int inicio = 0; inicio < linhas.size(); inicio += LINHAS_POR_PAGINA) {
            paginas.add(linhas.subList(inicio, Math.min(inicio + LINHAS_POR_PAGINA, linhas.size())));
        }
        if (paginas.isEmpty()) {
            paginas.add(List.of("Relatorio sem dados."));
        }
        return paginas;
    }

    private String conteudoPagina(List<String> linhas) {
        StringBuilder conteudo = new StringBuilder("BT\n/F1 9 Tf\n40 805 Td\n");
        for (String linha : linhas) {
            conteudo.append('(').append(escapePdf(linha)).append(") Tj\n0 -15 Td\n");
        }
        return conteudo.append("ET").toString();
    }

    private String escapePdf(String valor) {
        return texto(valor).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
    }

    private byte[] bytes(String valor) {
        return valor.getBytes(StandardCharsets.ISO_8859_1);
    }

    private String campo(Object valor) {
        String texto = texto(valor).replace("\"", "\"\"");
        return texto.contains(";") || texto.contains("\n") || texto.contains("\"") ? "\"" + texto + "\"" : texto;
    }

    private String numero(BigDecimal valor) {
        return valor == null ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
