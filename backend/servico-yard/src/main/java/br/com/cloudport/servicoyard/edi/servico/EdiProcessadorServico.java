package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.VermasMensagemDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.parser.BaplieParser;
import br.com.cloudport.servicoyard.edi.parser.CoprarCoarriParser;
import br.com.cloudport.servicoyard.edi.parser.CoprarCoarriParser.ResultadoParse;
import br.com.cloudport.servicoyard.edi.parser.VermasParser;
import br.com.cloudport.servicoyard.edi.parser.VermasParser.ResultadoVermas;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EdiProcessadorServico {

    private final BaplieParser baplieParser;
    private final CoprarCoarriParser coprarCoarriParser;
    private final VermasParser vermasParser;
    private final BayPlanServico bayPlanServico;

    public EdiProcessadorServico(BaplieParser baplieParser,
                                   CoprarCoarriParser coprarCoarriParser,
                                   VermasParser vermasParser,
                                   BayPlanServico bayPlanServico) {
        this.baplieParser = baplieParser;
        this.coprarCoarriParser = coprarCoarriParser;
        this.vermasParser = vermasParser;
        this.bayPlanServico = bayPlanServico;
    }

    public BayPlanRespostaDto processarBaplie(String conteudoEdifact) {
        validarTipoMensagem(conteudoEdifact, "BAPLIE");
        BayPlan bayPlan = baplieParser.parse(conteudoEdifact);
        if (bayPlan.getContainers().isEmpty()) {
            throw new IllegalArgumentException("BAPLIE: a mensagem nao possui containers validos.");
        }
        return bayPlanServico.processarBaplie(bayPlan);
    }

    public BayPlanRespostaDto processarCoprar(CoprarMensagemDto dto) {
        validarTipoMensagem(dto.getConteudoEdifact(), "COPRAR");
        ResultadoParse resultado = coprarCoarriParser.parse(dto.getConteudoEdifact());
        String codigoNavio = preferir(dto.getCodigoNavio(), resultado.getCodigoNavio());
        String codigoViagem = preferir(dto.getCodigoViagem(), resultado.getCodigoViagem());
        validarNavioViagem(codigoNavio, codigoViagem, "COPRAR");
        if (resultado.getContainers().isEmpty()) {
            throw new IllegalArgumentException("COPRAR: nenhum container valido foi encontrado.");
        }
        return bayPlanServico.processarCoprar(codigoNavio, codigoViagem, resultado.getContainers());
    }

    public BayPlanRespostaDto processarCoarri(CoarriMensagemDto dto) {
        validarTipoMensagem(dto.getConteudoEdifact(), "COARRI");
        ResultadoParse resultado = coprarCoarriParser.parse(dto.getConteudoEdifact());
        String codigoNavio = preferir(dto.getCodigoNavio(), resultado.getCodigoNavio());
        String codigoViagem = preferir(dto.getCodigoViagem(), resultado.getCodigoViagem());
        validarNavioViagem(codigoNavio, codigoViagem, "COARRI");
        if (resultado.getContainers().isEmpty()) {
            throw new IllegalArgumentException("COARRI: nenhuma confirmacao de container foi encontrada.");
        }
        return bayPlanServico.processarCoarri(codigoNavio, codigoViagem, resultado.getContainers());
    }

    public BayPlanRespostaDto processarVermas(VermasMensagemDto dto) {
        ResultadoVermas resultado = vermasParser.parse(dto.getConteudoEdifact());
        String codigoNavio = preferir(dto.getCodigoNavio(), resultado.codigoNavio());
        String codigoViagem = preferir(dto.getCodigoViagem(), resultado.codigoViagem());
        validarNavioViagem(codigoNavio, codigoViagem, "VERMAS");
        return bayPlanServico.processarVermas(codigoNavio, codigoViagem, resultado.pesos());
    }

    private void validarTipoMensagem(String conteudoEdifact, String tipoEsperado) {
        if (!StringUtils.hasText(conteudoEdifact)) {
            throw new IllegalArgumentException(tipoEsperado + ": conteudo EDIFACT vazio.");
        }
        String conteudo = conteudoEdifact.toUpperCase();
        if (!conteudo.contains("UNH+") || !conteudo.contains(tipoEsperado)) {
            throw new IllegalArgumentException(
                    tipoEsperado + ": cabecalho UNH da mensagem nao corresponde ao tipo esperado."
            );
        }
    }

    private String preferir(String informado, String extraido) {
        return StringUtils.hasText(informado) ? informado.trim() : extraido;
    }

    private void validarNavioViagem(String codigoNavio, String codigoViagem, String tipoMensagem) {
        if (!StringUtils.hasText(codigoNavio) || !StringUtils.hasText(codigoViagem)) {
            throw new IllegalArgumentException(
                    tipoMensagem + ": codigo do navio e codigo da viagem sao obrigatorios."
            );
        }
    }
}
