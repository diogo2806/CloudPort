package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLacreStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoLacreStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.LacreOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.LacreOperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarLacreRequest;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LacreStuffUnstuffServico {

    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;

    public LacreStuffUnstuffServico(OperacaoStuffUnstuffRepositorio operacaoRepositorio) {
        this.operacaoRepositorio = operacaoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<LacreOperacaoResposta> listar(UUID operacaoId) {
        OperacaoStuffUnstuff operacao = operacaoRepositorio.findDetalhadaById(operacaoId)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
        return operacao.getLacres().stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional
    public LacreOperacaoResposta registrar(UUID operacaoId, RegistrarLacreRequest request) {
        OperacaoStuffUnstuff operacao = operacaoRepositorio.findComBloqueioById(operacaoId)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
        validarOperacaoAberta(operacao);

        LacreOperacaoStuffUnstuff existente = operacao.getLacres().stream()
                .filter(lacre -> lacre.getCommandId().equals(request.commandId()))
                .findFirst()
                .orElse(null);
        if (existente != null) {
            validarRepeticaoEquivalente(existente, request);
            return mapear(existente);
        }

        String numero = normalizar(request.numeroLacre());
        String substituido = normalizar(request.numeroLacreSubstituido());
        String lacreAtivo = obterLacreAtivo(operacao);
        validarTransicao(request.tipoEvento(), numero, substituido, lacreAtivo);

        boolean divergencia = request.divergencia();
        if (request.tipoEvento() == TipoEventoLacreStuffUnstuff.CONFERIDO
                && lacreAtivo != null && !lacreAtivo.equals(numero)) {
            divergencia = true;
        }
        if (request.overrideAutorizado() && (request.motivo() == null || request.motivo().isBlank())) {
            throw conflito("Override de lacre exige motivo informado.");
        }

        LacreOperacaoStuffUnstuff lacre = new LacreOperacaoStuffUnstuff();
        lacre.setCommandId(request.commandId());
        lacre.setNumeroLacre(numero);
        lacre.setNumeroLacreSubstituido(substituido);
        lacre.setTipoEvento(request.tipoEvento());
        lacre.setStatus(definirStatus(request.tipoEvento(), divergencia));
        lacre.setOperador(request.operador());
        lacre.setCorrelationId(request.correlationId());
        lacre.setMotivo(request.motivo());
        lacre.setDivergenciaAberta(divergencia && !request.overrideAutorizado());
        lacre.setOverrideAutorizado(request.overrideAutorizado());
        operacao.adicionarLacre(lacre);

        if (request.tipoEvento() == TipoEventoLacreStuffUnstuff.CONFERIDO && !divergencia) {
            fecharDivergencias(operacao, false);
        } else if (request.overrideAutorizado()) {
            fecharDivergencias(operacao, true);
        }

        operacao.registrarEvento(
                mapearTipoEvento(request.tipoEvento()),
                request.operador(),
                request.correlationId(),
                descricaoEvento(request, divergencia));
        operacaoRepositorio.saveAndFlush(operacao);
        return mapear(lacre);
    }

    private void validarOperacaoAberta(OperacaoStuffUnstuff operacao) {
        if (operacao.getStatus() == StatusOperacaoStuffUnstuff.CONCLUIDA
                || operacao.getStatus() == StatusOperacaoStuffUnstuff.CANCELADA) {
            throw conflito("Operação encerrada não aceita eventos de lacre.");
        }
    }

    private void validarRepeticaoEquivalente(LacreOperacaoStuffUnstuff existente, RegistrarLacreRequest request) {
        if (existente.getTipoEvento() != request.tipoEvento()
                || !existente.getNumeroLacre().equals(normalizar(request.numeroLacre()))
                || !iguais(existente.getNumeroLacreSubstituido(), normalizar(request.numeroLacreSubstituido()))) {
            throw conflito("O commandId do lacre já foi utilizado com conteúdo diferente.");
        }
    }

    private void validarTransicao(
            TipoEventoLacreStuffUnstuff tipo,
            String numero,
            String substituido,
            String lacreAtivo) {
        if (tipo == TipoEventoLacreStuffUnstuff.ROMPIDO && (lacreAtivo == null || !lacreAtivo.equals(numero))) {
            throw conflito("O lacre rompido deve corresponder ao lacre ativo da operação.");
        }
        if (tipo == TipoEventoLacreStuffUnstuff.SUBSTITUIDO) {
            if (substituido == null || substituido.isBlank()) {
                throw conflito("A substituição exige o número do lacre substituído.");
            }
            if (lacreAtivo == null || !lacreAtivo.equals(substituido)) {
                throw conflito("O lacre substituído deve corresponder ao lacre ativo da operação.");
            }
            if (numero.equals(substituido)) {
                throw conflito("O novo lacre deve ser diferente do lacre substituído.");
            }
        }
    }

    private String obterLacreAtivo(OperacaoStuffUnstuff operacao) {
        List<LacreOperacaoStuffUnstuff> lacres = operacao.getLacres();
        for (int indice = lacres.size() - 1; indice >= 0; indice--) {
            LacreOperacaoStuffUnstuff lacre = lacres.get(indice);
            if (lacre.getTipoEvento() == TipoEventoLacreStuffUnstuff.APLICADO
                    || lacre.getTipoEvento() == TipoEventoLacreStuffUnstuff.SUBSTITUIDO
                    || lacre.getTipoEvento() == TipoEventoLacreStuffUnstuff.CONFERIDO) {
                return lacre.getNumeroLacre();
            }
            if (lacre.getTipoEvento() == TipoEventoLacreStuffUnstuff.ROMPIDO) {
                return null;
            }
        }
        return operacao.getLacreInicial() == null ? null : normalizar(operacao.getLacreInicial());
    }

    private void fecharDivergencias(OperacaoStuffUnstuff operacao, boolean override) {
        operacao.getLacres().stream()
                .filter(LacreOperacaoStuffUnstuff::isDivergenciaAberta)
                .forEach(lacre -> {
                    lacre.setDivergenciaAberta(false);
                    if (override) {
                        lacre.setOverrideAutorizado(true);
                    }
                });
    }

    private StatusLacreStuffUnstuff definirStatus(TipoEventoLacreStuffUnstuff tipo, boolean divergencia) {
        if (divergencia) {
            return StatusLacreStuffUnstuff.DIVERGENTE;
        }
        return StatusLacreStuffUnstuff.valueOf(tipo.name());
    }

    private TipoEventoStuffUnstuff mapearTipoEvento(TipoEventoLacreStuffUnstuff tipo) {
        return TipoEventoStuffUnstuff.valueOf("LACRE_" + tipo.name());
    }

    private String descricaoEvento(RegistrarLacreRequest request, boolean divergencia) {
        String descricao = request.tipoEvento().name().toLowerCase(Locale.ROOT)
                + " lacre " + normalizar(request.numeroLacre());
        if (request.numeroLacreSubstituido() != null && !request.numeroLacreSubstituido().isBlank()) {
            descricao += " em substituição ao lacre " + normalizar(request.numeroLacreSubstituido());
        }
        if (divergencia) {
            descricao += request.overrideAutorizado()
                    ? "; divergência autorizada por override motivado"
                    : "; divergência aberta bloqueando a conclusão";
        }
        return descricao + ".";
    }

    private LacreOperacaoResposta mapear(LacreOperacaoStuffUnstuff lacre) {
        return new LacreOperacaoResposta(
                lacre.getId(),
                lacre.getCommandId(),
                lacre.getNumeroLacre(),
                lacre.getNumeroLacreSubstituido(),
                lacre.getTipoEvento(),
                lacre.getStatus(),
                lacre.getOperador(),
                lacre.getCorrelationId(),
                lacre.getMotivo(),
                lacre.isDivergenciaAberta(),
                lacre.isOverrideAutorizado(),
                lacre.getOcorridoEm());
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private boolean iguais(String primeiro, String segundo) {
        return primeiro == null ? segundo == null : primeiro.equals(segundo);
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
