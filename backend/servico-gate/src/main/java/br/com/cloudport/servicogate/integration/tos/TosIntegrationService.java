package br.com.cloudport.servicogate.integration.tos;

import br.com.cloudport.servicogate.app.gestor.dto.TosBookingInfo;
import br.com.cloudport.servicogate.app.gestor.dto.TosContainerStatus;
import br.com.cloudport.servicogate.app.gestor.dto.TosSyncResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosCustomsReleaseResponse;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TosIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TosIntegrationService.class);

    private final TosClient tosClient;
    private final TosResponseAdapter adapter;
    private final CacheManager cacheManager;

    public TosIntegrationService(TosClient tosClient,
                                 TosResponseAdapter adapter,
                                 CacheManager cacheManager) {
        this.tosClient = tosClient;
        this.adapter = adapter;
        this.cacheManager = cacheManager;
    }

    @Cacheable(cacheNames = TosCacheNames.BOOKING, key = "#bookingNumber")
    public TosBookingInfo obterBookingInfo(String bookingNumber) {
        TosBookingInfo info = adapter.toBookingInfo(tosClient.buscarBooking(bookingNumber));
        if (info != null) {
            LOGGER.info("event=tos.booking.sync resource=booking identifier={} status=success liberado={} correlationId={}",
                    TosObservabilidadeSegura.mascararIdentificador(info.getBookingNumber()),
                    info.isLiberado(),
                    TosObservabilidadeSegura.obterCorrelationId());
        }
        return info;
    }

    @Cacheable(cacheNames = TosCacheNames.CONTAINER_STATUS, key = "#containerNumber")
    public TosContainerStatus obterStatusContainer(String containerNumber) {
        TosContainerStatus status = adapter.toContainerStatus(
                tosClient.buscarStatusContainer(containerNumber),
                obterLiberacaoAduaneira(containerNumber));
        if (status != null) {
            LOGGER.info("event=tos.container.sync resource=container-status identifier={} status=success gateLiberado={} customsLiberado={} correlationId={}",
                    TosObservabilidadeSegura.mascararIdentificador(status.getContainerNumber()),
                    status.isGateLiberado(),
                    status.isLiberacaoAduaneira(),
                    TosObservabilidadeSegura.obterCorrelationId());
        }
        return status;
    }

    @Cacheable(cacheNames = TosCacheNames.CUSTOMS_RELEASE, key = "#containerNumber")
    public TosCustomsReleaseResponse obterLiberacaoAduaneira(String containerNumber) {
        return tosClient.buscarLiberacaoAduaneira(containerNumber);
    }

    public void limparCaches(String containerNumber) {
        if (!StringUtils.hasText(containerNumber)) {
            return;
        }
        evict(TosCacheNames.BOOKING, containerNumber);
        evict(TosCacheNames.CONTAINER_STATUS, containerNumber);
        evict(TosCacheNames.CUSTOMS_RELEASE, containerNumber);
        LOGGER.info("event=tos.cache.evict resource=cache identifier={} status=success correlationId={}",
                TosObservabilidadeSegura.mascararIdentificador(containerNumber),
                TosObservabilidadeSegura.obterCorrelationId());
    }

    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    public void validarAgendamentoParaCriacao(String bookingNumber, TipoOperacao tipoOperacao) {
        if (!StringUtils.hasText(bookingNumber)) {
            return;
        }
        TosBookingInfo bookingInfo = obterBookingInfo(bookingNumber);
        String identificadorMascarado = TosObservabilidadeSegura.mascararIdentificador(bookingNumber);
        if (bookingInfo == null) {
            throw erroOperacional(
                    "booking",
                    identificadorMascarado,
                    "TOS_BOOKING_NAO_LOCALIZADO",
                    "Booking " + identificadorMascarado + " não localizado no TOS.");
        }
        if (!bookingInfo.isLiberado()) {
            throw erroOperacional(
                    "booking",
                    identificadorMascarado,
                    "TOS_BOOKING_BLOQUEADO",
                    "TOS negou a criação do agendamento para o booking " + identificadorMascarado + ".");
        }
        LOGGER.info("event=tos.booking.validated resource=booking identifier={} status=success tipoOperacao={} liberado={} correlationId={}",
                identificadorMascarado,
                tipoOperacao,
                bookingInfo.isLiberado(),
                TosObservabilidadeSegura.obterCorrelationId());
    }

    public TosContainerStatus validarParaEntrada(Agendamento agendamento) {
        String identificador = agendamento.getCodigo();
        if (!StringUtils.hasText(identificador)) {
            return null;
        }
        String identificadorMascarado = TosObservabilidadeSegura.mascararIdentificador(identificador);
        TosContainerStatus status = obterStatusContainer(identificador);
        if (status == null) {
            throw erroOperacional(
                    "container-status",
                    identificadorMascarado,
                    "TOS_CONTAINER_NAO_LOCALIZADO",
                    "Status do contêiner " + identificadorMascarado + " não localizado no TOS.");
        }
        if (!status.isGateLiberado()) {
            throw erroOperacional(
                    "container-status",
                    identificadorMascarado,
                    "TOS_GATE_BLOQUEADO",
                    "TOS bloqueou a entrada para o contêiner " + identificadorMascarado + ".");
        }
        if (!status.isLiberacaoAduaneira()) {
            throw erroOperacional(
                    "customs-release",
                    identificadorMascarado,
                    "TOS_PENDENCIA_ADUANEIRA",
                    "TOS indicou pendência aduaneira para o contêiner " + identificadorMascarado + ".");
        }
        LOGGER.info("event=tos.container.validated resource=container-status identifier={} status=success agendamento={} customsLiberado={} correlationId={}",
                identificadorMascarado,
                agendamento.getId(),
                status.isLiberacaoAduaneira(),
                TosObservabilidadeSegura.obterCorrelationId());
        return status;
    }

    public TosSyncResponse sincronizar(Agendamento agendamento) {
        String identificador = agendamento.getCodigo();
        boolean possuiIdentificador = StringUtils.hasText(identificador);
        if (possuiIdentificador) {
            limparCaches(identificador);
        }
        TosBookingInfo bookingInfo = possuiIdentificador ? obterBookingInfo(identificador) : null;
        TosContainerStatus containerStatus = possuiIdentificador ? obterStatusContainer(identificador) : null;
        LOGGER.info("event=tos.sync.completed resource=sync identifier={} status=success agendamento={} bookingPresente={} containerPresente={} correlationId={}",
                TosObservabilidadeSegura.mascararIdentificador(identificador),
                agendamento.getId(),
                bookingInfo != null,
                containerStatus != null,
                TosObservabilidadeSegura.obterCorrelationId());
        return new TosSyncResponse(agendamento.getId(), bookingInfo, containerStatus);
    }

    private TosIntegrationException erroOperacional(String recurso,
                                                     String identificadorMascarado,
                                                     String codigoErro,
                                                     String mensagem) {
        String correlationId = TosObservabilidadeSegura.obterCorrelationId();
        int status = HttpStatus.UNPROCESSABLE_ENTITY.value();
        LOGGER.warn("event=tos.validation.error resource={} identifier={} status={} errorCode={} correlationId={}",
                recurso, identificadorMascarado, status, codigoErro, correlationId);
        return new TosIntegrationException(
                mensagem,
                status,
                recurso,
                identificadorMascarado,
                codigoErro,
                correlationId);
    }
}
