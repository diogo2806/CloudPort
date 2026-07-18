package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dto.AtribuicaoReplanejamentoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OtimizacaoGlobalNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.servico.AplicacaoPlanoOtimizadoNavioPatioServico.ResultadoAplicacaoPlano;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class IntegracaoNavioPatioOtimizadaServico extends IntegracaoNavioPatioServico {

    private final AplicacaoPlanoOtimizadoNavioPatioServico aplicacaoPlanoServico;
    private final OtimizacaoGlobalNavioPatioServico otimizacaoGlobalServico;

    public IntegracaoNavioPatioOtimizadaServico(
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            VisitaNavioServico visitaServico,
            PlanoEstivaNavioServico planoServico,
            ReservaPatioNavioServico reservaPatioServico,
            ValidadorIntegracaoNavioPatioServico validador,
            SincronizadorStatusNavioPatioServico sincronizador,
            OrdemPatioYardCliente ordemPatioYardCliente,
            AplicacaoPlanoOtimizadoNavioPatioServico aplicacaoPlanoServico,
            OtimizacaoGlobalNavioPatioServico otimizacaoGlobalServico,
            @Value("${cloudport.integracao.yard.contingencia-consultas-enabled:false}")
            boolean contingenciaConsultasYardHabilitada
    ) {
        super(
                itemRepositorio,
                reservaRepositorio,
                visitaServico,
                planoServico,
                reservaPatioServico,
                validador,
                sincronizador,
                ordemPatioYardCliente,
                contingenciaConsultasYardHabilitada);
        this.aplicacaoPlanoServico = aplicacaoPlanoServico;
        this.otimizacaoGlobalServico = otimizacaoGlobalServico;
    }

    @Override
    public ResultadoReplanejamentoPatioNavioDTO replanejarPatioDaVisita(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando
    ) {
        OtimizacaoGlobalNavioPatioDTO planoPreparado = otimizacaoGlobalServico.otimizar(
                visitaId,
                comando);
        ResultadoAplicacaoPlano resultado = aplicacaoPlanoServico.replanejar(visitaId, comando);
        Map<String, Object> plano = planoPreparado.planoOtimizado() == null
                ? Map.of()
                : planoPreparado.planoOtimizado();
        return new ResultadoReplanejamentoPatioNavioDTO(
                resultado.reservas(),
                listarOrdensDaVisita(visitaId),
                resultado.economiaPercentual(),
                resultado.riscoRehandle(),
                resultado.alertasImpeditivos(),
                resultado.itensNaoReplanejados(),
                resultado.planoId(),
                resultado.versaoPlano(),
                resultado.distanciaOriginal(),
                resultado.distanciaOtimizada(),
                converterAtribuicoes(plano.get("atribuicoesReplanejamento")),
                converterMemoria(plano.get("memoriaCalculo")),
                converterTextos(plano.get("justificativas")),
                texto(plano.get("assinaturaEntrada")),
                decimal(plano.get("pontuacaoTotal")));
    }

    private List<AtribuicaoReplanejamentoNavioPatioDTO> converterAtribuicoes(Object valor) {
        List<AtribuicaoReplanejamentoNavioPatioDTO> resultado = new ArrayList<>();
        for (Map<String, Object> item : listaMapas(valor)) {
            resultado.add(new AtribuicaoReplanejamentoNavioPatioDTO(
                    texto(item.get("codigoContainer")),
                    texto(item.get("movimento")),
                    inteiro(item.get("linhaOriginal")),
                    inteiro(item.get("colunaOriginal")),
                    texto(item.get("camadaOriginal")),
                    inteiro(item.get("linhaProposta")),
                    inteiro(item.get("colunaProposta")),
                    texto(item.get("camadaProposta")),
                    texto(item.get("blocoProposto")),
                    texto(item.get("equipamentoId")),
                    inteiro(item.get("sequenciaPlano")),
                    decimal(item.get("scoreTotal")),
                    inteiro(item.get("distancia")),
                    inteiro(item.get("rehandlesEstimados")),
                    converterTextos(item.get("justificativas"))));
        }
        return List.copyOf(resultado);
    }

    private Map<String, BigDecimal> converterMemoria(Object valor) {
        if (!(valor instanceof Map<?, ?> mapa)) {
            return Map.of();
        }
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        mapa.forEach((chave, conteudo) -> {
            BigDecimal numero = decimal(conteudo);
            if (numero != null) {
                resultado.put(String.valueOf(chave), numero);
            }
        });
        return Map.copyOf(resultado);
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

    private List<String> converterTextos(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        return lista.stream().map(String::valueOf).toList();
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

    private BigDecimal decimal(Object valor) {
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(valor));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
