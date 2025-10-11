package br.com.cloudport.servicogate.integration.tos;

import br.com.cloudport.servicogate.dto.TosBookingInfo;
import br.com.cloudport.servicogate.dto.TosContainerStatus;
import br.com.cloudport.servicogate.dto.TosSyncResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosCustomsReleaseResponse;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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
            LOGGER.info("event=tos.booking.sync booking={} liberado={} vessel={} voyage={}",
                    info.getBookingNumber(), info.isLiberado(), info.getVessel(), info.getVoyage());
        }
        return info;
    }

    @Cacheable(cacheNames = TosCacheNames.CONTAINER_STATUS, key = "#containerNumber")
    public TosContainerStatus obterStatusContainer(String containerNumber) {
        TosContainerStatus status = adapter.toContainerStatus(
                tosClient.buscarStatusContainer(containerNumber),
                obterLiberacaoAduaneira(containerNumber));
        if (status != null) {
            LOGGER.info("event=tos.container.sync container={} status={} gateLiberado={} customsLiberado={} motivo={}",
                    status.getContainerNumber(), status.getStatus(), status.isGateLiberado(),
                    status.isLiberacaoAduaneira(), status.getMotivoRestricao());
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
        LOGGER.info("event=tos.cache.evict identifier={}", containerNumber);
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
        if (bookingInfo == null) {
            throw new TosIntegrationException(String.format("Booking %s não localizado no TOS", bookingNumber));
        }
        if (!bookingInfo.isLiberado()) {
            String motivo = Optional.ofNullable(bookingInfo.getMotivoRestricao())
                    .filter(StringUtils::hasText)
                    .orElse("Motivo não informado pelo TOS");
            throw new TosIntegrationException(String.format(
                    "TOS negou criação do agendamento para booking %s (%s): %s",
                    bookingNumber, tipoOperacao, motivo));
        }
        LOGGER.info("event=tos.booking.validated booking={} tipoOperacao={} liberado={}",
                bookingInfo.getBookingNumber(), tipoOperacao, bookingInfo.isLiberado());
    }

    public TosContainerStatus validarParaEntrada(Agendamento agendamento) {
        String identificador = agendamento.getCodigo();
        if (!StringUtils.hasText(identificador)) {
            return null;
        }
        TosContainerStatus status = obterStatusContainer(identificador);
        if (status == null) {
            throw new TosIntegrationException(String.format("Status do contêiner %s não localizado no TOS",
                    identificador));
        }
        if (!status.isGateLiberado()) {
            String motivo = Optional.ofNullable(status.getMotivoRestricao())
                    .filter(StringUtils::hasText)
                    .orElse("TOS não informou motivo");
            throw new TosIntegrationException(String.format(
                    "TOS bloqueou o gate para o contêiner %s: %s", identificador, motivo));
        }
        if (!status.isLiberacaoAduaneira()) {
            String motivo = Optional.ofNullable(status.getMotivoRestricao())
                    .filter(StringUtils::hasText)
                    .orElse("Contêiner sem liberação aduaneira no TOS");
            throw new TosIntegrationException(String.format(
                    "TOS indicou pendência aduaneira para o contêiner %s: %s", identificador, motivo));
        }
        LOGGER.info("event=tos.container.validated agendamento={} container={} status={} customsLiberado={}",
                agendamento.getId(), status.getContainerNumber(), status.getStatus(), status.isLiberacaoAduaneira());
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
        LOGGER.info("event=tos.sync.completed agendamento={} booking={} container={}", agendamento.getId(),
                bookingInfo != null ? bookingInfo.getBookingNumber() : null,
                containerStatus != null ? containerStatus.getContainerNumber() : null);
        return new TosSyncResponse(agendamento.getId(), bookingInfo, containerStatus);
    }
}
