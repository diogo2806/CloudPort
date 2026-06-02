package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest;
import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ConfirmacaoBarcodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmacaoBarcodeService.class);

    private final GatePassRepository gatePassRepository;
    private final GateEventRepository gateEventRepository;
    private final AgendamentoRealtimeService agendamentoRealtimeService;

    public ConfirmacaoBarcodeService(GatePassRepository gatePassRepository,
                                     GateEventRepository gateEventRepository,
                                     AgendamentoRealtimeService agendamentoRealtimeService) {
        this.gatePassRepository = gatePassRepository;
        this.gateEventRepository = gateEventRepository;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
    }

    public ConfirmacaoBarcodeResponse confirmarBarcode(ConfirmacaoBarcodeRequest request) {
        GatePass gatePass = obterGatePassPorToken(request.getTokenGatePass());
        validarGatePassParaConfirmacao(gatePass);

        LocalDateTime dataConfirmacao = request.getDataConfirmacao() != null
                ? request.getDataConfirmacao()
                : LocalDateTime.now();

        if (Boolean.TRUE.equals(request.getConfirmado())) {
            return confirmarBarcodeComSucesso(gatePass, request.getCodigoBarcode(),
                    request.getDispositivoDmtId(), dataConfirmacao);
        } else {
            return rejeitarBarcode(gatePass, request.getCodigoBarcode(),
                    request.getMotivo(), request.getDispositivoDmtId(), dataConfirmacao);
        }
    }

    private ConfirmacaoBarcodeResponse confirmarBarcodeComSucesso(GatePass gatePass,
                                                                   String codigoBarcode,
                                                                   String dispositivoDmtId,
                                                                   LocalDateTime dataConfirmacao) {
        gatePass.setCodigoBarcode(codigoBarcode);
        gatePass.setDataConfirmacaoBarcode(dataConfirmacao);
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.CONFIRMADO);
        gatePass.setMotivoRejeicaoBarcode(null);
        gatePass.setStatus(StatusGate.LIBERADO);

        GatePass salvo = gatePassRepository.save(gatePass);
        registrarEvento(salvo, StatusGate.LIBERADO,
                String.format("Barcode confirmado pelo DMT %s: %s", dispositivoDmtId, codigoBarcode));

        LOGGER.info("event=barcode.confirmado gatePassId={} barcode={} dmt={} timestamp={}",
                salvo.getId(), codigoBarcode, dispositivoDmtId, dataConfirmacao);

        agendamentoRealtimeService.notificarStatus(salvo.getAgendamento());

        return new ConfirmacaoBarcodeResponse(salvo.getId(), salvo.getToken(), codigoBarcode,
                StatusConfirmacaoBarcode.CONFIRMADO.toString(), dataConfirmacao,
                "Barcode confirmado com sucesso");
    }

    private ConfirmacaoBarcodeResponse rejeitarBarcode(GatePass gatePass,
                                                        String codigoBarcode,
                                                        String motivo,
                                                        String dispositivoDmtId,
                                                        LocalDateTime dataConfirmacao) {
        gatePass.setCodigoBarcode(codigoBarcode);
        gatePass.setDataConfirmacaoBarcode(dataConfirmacao);
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.REJEITADO);
        gatePass.setMotivoRejeicaoBarcode(motivo);
        gatePass.setStatus(StatusGate.RETIDO);

        GatePass salvo = gatePassRepository.save(gatePass);
        String observacao = StringUtils.hasText(motivo)
                ? String.format("Barcode rejeitado pelo DMT %s: %s", dispositivoDmtId, motivo)
                : String.format("Barcode rejeitado pelo DMT %s", dispositivoDmtId);
        registrarEvento(salvo, StatusGate.RETIDO, observacao);

        LOGGER.warn("event=barcode.rejeitado gatePassId={} barcode={} dmt={} motivo={} timestamp={}",
                salvo.getId(), codigoBarcode, dispositivoDmtId, motivo, dataConfirmacao);

        agendamentoRealtimeService.notificarStatus(salvo.getAgendamento());

        return new ConfirmacaoBarcodeResponse(salvo.getId(), salvo.getToken(), codigoBarcode,
                StatusConfirmacaoBarcode.REJEITADO.toString(), dataConfirmacao,
                String.format("Barcode rejeitado: %s", motivo != null ? motivo : "sem motivo informado"));
    }

    public ConfirmacaoBarcodeResponse registrarTimeoutBarcode(String tokenGatePass, String dispositivoDmtId) {
        GatePass gatePass = obterGatePassPorToken(tokenGatePass);

        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.TIMEOUT);
        gatePass.setDataConfirmacaoBarcode(LocalDateTime.now());
        gatePass.setMotivoRejeicaoBarcode("Timeout aguardando confirmação do DMT");
        gatePass.setStatus(StatusGate.RETIDO);

        GatePass salvo = gatePassRepository.save(gatePass);
        registrarEvento(salvo, StatusGate.RETIDO,
                String.format("Timeout de confirmação de barcode do DMT %s (aguardou resposta por muito tempo)",
                        dispositivoDmtId));

        LOGGER.warn("event=barcode.timeout gatePassId={} dmt={} timestamp={}",
                salvo.getId(), dispositivoDmtId, LocalDateTime.now());

        agendamentoRealtimeService.notificarStatus(salvo.getAgendamento());

        return new ConfirmacaoBarcodeResponse(salvo.getId(), salvo.getToken(), null,
                StatusConfirmacaoBarcode.TIMEOUT.toString(), LocalDateTime.now(),
                "Timeout na confirmação de barcode do dispositivo DMT");
    }

    private GatePass obterGatePassPorToken(String token) {
        return gatePassRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "GatePass com token %s não encontrado", token)));
    }

    private void validarGatePassParaConfirmacao(GatePass gatePass) {
        if (gatePass.getStatus() == StatusGate.FINALIZADO) {
            throw new BusinessException("Gate pass já foi finalizado, não pode mais confirmar barcode");
        }
        if (gatePass.getStatusConfirmacaoBarcode() != null &&
            gatePass.getStatusConfirmacaoBarcode() != StatusConfirmacaoBarcode.PENDENTE) {
            throw new BusinessException("Barcode para este gate pass já foi confirmado/rejeitado");
        }
    }

    private void registrarEvento(GatePass gatePass, StatusGate status, String observacao) {
        GateEvent evento = new GateEvent();
        evento.setGatePass(gatePass);
        evento.setStatus(status);
        evento.setObservacao(observacao);
        evento.setUsuarioResponsavel("sistema-barcode-validation");
        evento.setRegistradoEm(LocalDateTime.now());
        gateEventRepository.save(evento);
    }
}
