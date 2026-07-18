package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.servico.AplicacaoPlanoOtimizadoNavioPatioServico.ResultadoAplicacaoPlano;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class IntegracaoNavioPatioOtimizadaServico extends IntegracaoNavioPatioServico {

    private final AplicacaoPlanoOtimizadoNavioPatioServico aplicacaoPlanoServico;

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
    }

    @Override
    public ResultadoReplanejamentoPatioNavioDTO replanejarPatioDaVisita(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando
    ) {
        ResultadoAplicacaoPlano resultado = aplicacaoPlanoServico.replanejar(visitaId, comando);
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
                resultado.distanciaOtimizada());
    }
}
