package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.parser.BaplieParser;
import br.com.cloudport.servicoyard.edi.parser.CoprarCoarriParser;
import br.com.cloudport.servicoyard.edi.parser.CoprarCoarriParser.ResultadoParse;
import org.springframework.stereotype.Service;

/**
 * Ponto de entrada único para processar mensagens EDI.
 * Orquestra parser → BayPlanServico → publicação WebSocket.
 */
@Service
public class EdiProcessadorServico {

    private final BaplieParser baplieParser;
    private final CoprarCoarriParser coprarCoarriParser;
    private final BayPlanServico bayPlanServico;

    public EdiProcessadorServico(BaplieParser baplieParser,
                                  CoprarCoarriParser coprarCoarriParser,
                                  BayPlanServico bayPlanServico) {
        this.baplieParser = baplieParser;
        this.coprarCoarriParser = coprarCoarriParser;
        this.bayPlanServico = bayPlanServico;
    }

    /**
     * Processa um arquivo BAPLIE (texto EDIFACT completo).
     * Cria ou substitui o Bay Plan do navio/viagem.
     */
    public BayPlanRespostaDto processarBaplie(String conteudoEdifact) {
        BayPlan bayPlan = baplieParser.parse(conteudoEdifact);
        return bayPlanServico.processarBaplie(bayPlan);
    }

    /**
     * Processa uma mensagem COPRAR recebida.
     * Atualiza o Bay Plan existente com as novas instruções de carga.
     * Se não houver Bay Plan, cria um em RASCUNHO.
     */
    public BayPlanRespostaDto processarCoprar(CoprarMensagemDto dto) {
        String edifact = dto.getConteudoEdifact();
        ResultadoParse resultado = coprarCoarriParser.parse(edifact);

        // Código do navio e viagem podem vir no DTO (quando enviados por sistemas externos)
        // ou extraídos do segmento TDT do EDIFACT
        String codigoNavio = dto.getCodigoNavio() != null
                ? dto.getCodigoNavio()
                : resultado.getCodigoNavio();
        String codigoViagem = dto.getCodigoViagem() != null
                ? dto.getCodigoViagem()
                : resultado.getCodigoViagem();

        if (codigoNavio == null || codigoViagem == null) {
            throw new IllegalArgumentException(
                    "COPRAR: código do navio e viagem são obrigatórios (DTO ou segmento TDT)");
        }

        return bayPlanServico.processarCoprar(codigoNavio, codigoViagem,
                resultado.getContainers());
    }

    /**
     * Processa uma mensagem COARRI recebida.
     * Confirma as operações com horário real e atualiza status dos containers.
     */
    public BayPlanRespostaDto processarCoarri(CoarriMensagemDto dto) {
        ResultadoParse resultado = coprarCoarriParser.parse(dto.getConteudoEdifact());

        String codigoNavio = dto.getCodigoNavio() != null
                ? dto.getCodigoNavio()
                : resultado.getCodigoNavio();
        String codigoViagem = dto.getCodigoViagem() != null
                ? dto.getCodigoViagem()
                : resultado.getCodigoViagem();

        if (codigoNavio == null || codigoViagem == null) {
            throw new IllegalArgumentException(
                    "COARRI: código do navio e viagem são obrigatórios");
        }

        return bayPlanServico.processarCoarri(codigoNavio, codigoViagem,
                resultado.getContainers());
    }
}
