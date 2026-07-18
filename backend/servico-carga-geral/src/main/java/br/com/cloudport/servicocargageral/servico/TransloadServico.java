package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import br.com.cloudport.servicocargageral.dominio.OperacaoTransload.ItemTransload;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusTransload;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ItemTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ItemTransloadResposta;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.TransloadResposta;
import br.com.cloudport.servicocargageral.integracao.inventario.InventarioConteinerCliente;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransloadServico {

    private final TransloadTransacaoServico transacaoServico;
    private final InventarioConteinerCliente inventarioConteinerCliente;

    public TransloadServico(
            TransloadTransacaoServico transacaoServico,
            InventarioConteinerCliente inventarioConteinerCliente) {
        this.transacaoServico = transacaoServico;
        this.inventarioConteinerCliente = inventarioConteinerCliente;
    }

    public TransloadResposta executarTransload(ExecutarTransloadRequest request) {
        validarComando(request);
        OperacaoTransload operacao = iniciarOuRecuperar(request);
        validarReexecucaoEquivalente(operacao, request);

        if (operacao.getStatus() == StatusTransload.CANCELADO) {
            throw conflito("O commandId informado pertence a um transload cancelado: "
                    + operacao.getMotivoCancelamento());
        }
        if (operacao.getStatus() == StatusTransload.CONCLUIDO) {
            liberarReservasConcluidas(operacao);
            return mapear(operacao);
        }

        reservarUnidades(operacao);
        try {
            operacao = transacaoServico.aplicar(operacao.getId(), request);
        } catch (RuntimeException exception) {
            compensarFalhaDeAplicacao(operacao, exception);
            throw exception;
        }

        liberarReservasConcluidas(operacao);
        return mapear(operacao);
    }

    public TransloadResposta obter(UUID id) {
        return mapear(transacaoServico.obter(id));
    }

    private OperacaoTransload iniciarOuRecuperar(ExecutarTransloadRequest request) {
        try {
            return transacaoServico.iniciar(request);
        } catch (DataIntegrityViolationException exception) {
            return transacaoServico.buscarPorCommandId(request.commandId());
        }
    }

    private void reservarUnidades(OperacaoTransload operacao) {
        inventarioConteinerCliente.reservar(
                operacao.getUnidadeOrigem(),
                operacao.getReservaOrigemId(),
                operacao.getUsuario());
        inventarioConteinerCliente.reservar(
                operacao.getUnidadeDestino(),
                operacao.getReservaDestinoId(),
                operacao.getUsuario());
    }

    private void validarComando(ExecutarTransloadRequest request) {
        if (Objects.equals(
                normalizarCodigo(request.unidadeOrigem()),
                normalizarCodigo(request.unidadeDestino()))) {
            throw conflito("Unidades de origem e destino do transload devem ser diferentes.");
        }
        boolean possuiCodigoAvaria = possuiTexto(request.codigoAvaria());
        boolean possuiDescricaoAvaria = possuiTexto(request.descricaoAvaria());
        if (possuiCodigoAvaria != possuiDescricaoAvaria) {
            throw conflito("Código e descrição da avaria devem ser informados em conjunto.");
        }
    }

    private void validarReexecucaoEquivalente(
            OperacaoTransload operacao,
            ExecutarTransloadRequest request) {
        boolean equivalente = Objects.equals(
                        normalizarCodigo(operacao.getUnidadeOrigem()),
                        normalizarCodigo(request.unidadeOrigem()))
                && Objects.equals(
                        normalizarCodigo(operacao.getUnidadeDestino()),
                        normalizarCodigo(request.unidadeDestino()))
                && Objects.equals(normalizarCodigo(operacao.getLacreOrigem()), normalizarCodigo(request.lacreOrigem()))
                && Objects.equals(normalizarCodigo(operacao.getLacreDestino()), normalizarCodigo(request.lacreDestino()))
                && Objects.equals(limpar(operacao.getDivergencia()), limpar(request.divergencia()))
                && Objects.equals(normalizarCodigo(operacao.getCodigoAvaria()), normalizarCodigo(request.codigoAvaria()))
                && Objects.equals(limpar(operacao.getDescricaoAvaria()), limpar(request.descricaoAvaria()))
                && Objects.equals(limpar(operacao.getUsuario()), limpar(request.usuario()))
                && correlationIdEquivalente(operacao, request)
                && itensEquivalentes(operacao.getItens(), request.itens());
        if (!equivalente) {
            throw conflito("O commandId já foi utilizado com dados diferentes de transload.");
        }
    }

    private boolean correlationIdEquivalente(
            OperacaoTransload operacao,
            ExecutarTransloadRequest request) {
        if (!possuiTexto(operacao.getCorrelationId())) {
            return !possuiTexto(request.correlationId());
        }
        return Objects.equals(operacao.getCorrelationId(), correlationId(request));
    }

    private boolean itensEquivalentes(
            List<ItemTransload> persistidos,
            List<ItemTransloadRequest> recebidos) {
        if (persistidos.size() != recebidos.size()) {
            return false;
        }
        for (int indice = 0; indice < persistidos.size(); indice++) {
            ItemTransload persistido = persistidos.get(indice);
            ItemTransloadRequest recebido = recebidos.get(indice);
            if (!persistido.getLoteOrigemId().equals(recebido.loteOrigemId())
                    || !persistido.getLoteDestinoId().equals(recebido.loteDestinoId())
                    || !mesmoNumero(persistido.getQuantidade(), recebido.quantidade())
                    || !mesmoNumero(persistido.getVolumeM3(), recebido.volumeM3())
                    || !mesmoNumero(persistido.getPesoKg(), recebido.pesoKg())) {
                return false;
            }
        }
        return true;
    }

    private void compensarFalhaDeAplicacao(
            OperacaoTransload operacao,
            RuntimeException falhaOriginal) {
        List<RuntimeException> falhasCompensacao = new ArrayList<>();
        liberarComCaptura(
                operacao.getReservaDestinoId(),
                operacao.getUsuario(),
                "Transload cancelado antes da atualização atômica",
                "CANCELADA",
                falhasCompensacao);
        liberarComCaptura(
                operacao.getReservaOrigemId(),
                operacao.getUsuario(),
                "Transload cancelado antes da atualização atômica",
                "CANCELADA",
                falhasCompensacao);
        try {
            transacaoServico.cancelar(operacao.getId(), mensagem(falhaOriginal));
        } catch (RuntimeException exception) {
            falhasCompensacao.add(exception);
        }
        falhasCompensacao.forEach(falhaOriginal::addSuppressed);
    }

    private void liberarReservasConcluidas(OperacaoTransload operacao) {
        List<RuntimeException> falhas = new ArrayList<>();
        liberarComCaptura(
                operacao.getReservaOrigemId(),
                operacao.getUsuario(),
                "Transload concluído",
                "CONCLUIDA",
                falhas);
        liberarComCaptura(
                operacao.getReservaDestinoId(),
                operacao.getUsuario(),
                "Transload concluído",
                "CONCLUIDA",
                falhas);
        if (!falhas.isEmpty()) {
            ResponseStatusException exception = new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Transload concluído, mas a liberação de uma unidade está pendente. Reenvie o mesmo commandId.",
                    falhas.get(0));
            falhas.stream().skip(1).forEach(exception::addSuppressed);
            throw exception;
        }
    }

    private void liberarComCaptura(
            UUID reservaId,
            String usuario,
            String motivo,
            String resultado,
            List<RuntimeException> falhas) {
        try {
            inventarioConteinerCliente.liberar(reservaId, usuario, motivo, resultado);
        } catch (RuntimeException exception) {
            falhas.add(exception);
        }
    }

    private TransloadResposta mapear(OperacaoTransload operacao) {
        List<ItemTransloadResposta> itens = operacao.getItens().stream()
                .map(item -> new ItemTransloadResposta(
                        item.getLoteOrigemId(),
                        item.getLoteDestinoId(),
                        item.getQuantidade(),
                        item.getVolumeM3(),
                        item.getPesoKg()))
                .toList();
        return new TransloadResposta(
                operacao.getId(),
                operacao.getCommandId(),
                operacao.getUnidadeOrigem(),
                operacao.getUnidadeDestino(),
                operacao.getReservaOrigemId(),
                operacao.getReservaDestinoId(),
                operacao.getLacreOrigem(),
                operacao.getLacreDestino(),
                operacao.getDivergencia(),
                operacao.getCodigoAvaria(),
                operacao.getDescricaoAvaria(),
                operacao.getStatus(),
                operacao.getMotivoCancelamento(),
                operacao.getUsuario(),
                operacao.getCorrelationId(),
                operacao.getExecutadoEm(),
                itens);
    }

    private boolean mesmoNumero(BigDecimal primeiro, BigDecimal segundo) {
        return primeiro != null && segundo != null && primeiro.compareTo(segundo) == 0;
    }

    private String correlationId(ExecutarTransloadRequest request) {
        return request.correlationId() == null || request.correlationId().isBlank()
                ? request.commandId().toString()
                : request.correlationId().trim();
    }

    private String normalizarCodigo(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    private String limpar(String valor) {
        return possuiTexto(valor) ? valor.trim() : null;
    }

    private boolean possuiTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String mensagem(RuntimeException exception) {
        String mensagem = exception.getMessage();
        return mensagem == null || mensagem.isBlank() ? exception.getClass().getSimpleName() : mensagem;
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
