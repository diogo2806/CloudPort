package br.com.cloudport.servicogate.app.gestor;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BarcodeConfirmacaoController.class)
class BarcodeConfirmacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfirmacaoBarcodeService confirmacaoBarcodeService;

    @Test
    void deveConfirmarBarcodeComSucesso() throws Exception {
        ConfirmacaoBarcodeResponse response = new ConfirmacaoBarcodeResponse(
                1L, "token-xyz", "CONT123456",
                StatusConfirmacaoBarcode.CONFIRMADO.toString(), LocalDateTime.now(),
                "Barcode confirmado com sucesso"
        );

        Map<String, Object> request = new HashMap<>();
        request.put("tokenGatePass", "token-xyz");
        request.put("codigoBarcode", "CONT123456");
        request.put("confirmado", true);
        request.put("dispositivoDmtId", "DMT-001");

        when(confirmacaoBarcodeService.confirmarBarcode(any())).thenReturn(response);

        mockMvc.perform(post("/gate/barcode/confirmar")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatePassId").value(1))
                .andExpect(jsonPath("$.tokenGatePass").value("token-xyz"))
                .andExpect(jsonPath("$.codigoBarcode").value("CONT123456"))
                .andExpect(jsonPath("$.statusConfirmacao").value("CONFIRMADO"))
                .andExpect(jsonPath("$.mensagem").value(containsString("sucesso")));

        verify(confirmacaoBarcodeService).confirmarBarcode(any());
    }

    @Test
    void deveRejeitarBarcodeComMotivo() throws Exception {
        ConfirmacaoBarcodeResponse response = new ConfirmacaoBarcodeResponse(
                1L, "token-xyz", "CONT-INCORRETO",
                StatusConfirmacaoBarcode.REJEITADO.toString(), LocalDateTime.now(),
                "Barcode rejeitado: Barcode não corresponde"
        );

        Map<String, Object> request = new HashMap<>();
        request.put("tokenGatePass", "token-xyz");
        request.put("codigoBarcode", "CONT-INCORRETO");
        request.put("confirmado", false);
        request.put("motivo", "Barcode não corresponde");
        request.put("dispositivoDmtId", "DMT-001");

        when(confirmacaoBarcodeService.confirmarBarcode(any())).thenReturn(response);

        mockMvc.perform(post("/gate/barcode/confirmar")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusConfirmacao").value("REJEITADO"))
                .andExpect(jsonPath("$.mensagem").value(containsString("rejeitado")));
    }

    @Test
    void deveValidarCamposObrigatorios() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("tokenGatePass", "");
        request.put("codigoBarcode", "CONT123456");
        request.put("confirmado", true);

        mockMvc.perform(post("/gate/barcode/confirmar")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRegistrarTimeoutComSucesso() throws Exception {
        ConfirmacaoBarcodeResponse response = new ConfirmacaoBarcodeResponse(
                1L, "token-xyz", null,
                StatusConfirmacaoBarcode.TIMEOUT.toString(), LocalDateTime.now(),
                "Timeout na confirmação de barcode do dispositivo DMT"
        );

        Map<String, Object> request = new HashMap<>();
        request.put("tokenGatePass", "token-xyz");
        request.put("dispositivoDmtId", "DMT-001");

        when(confirmacaoBarcodeService.registrarTimeoutBarcode(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/gate/barcode/timeout")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PORTO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.statusConfirmacao").value("TIMEOUT"));
    }

    @Test
    void deveNegartimeoutSemPermissao() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("tokenGatePass", "token-xyz");
        request.put("dispositivoDmtId", "DMT-001");

        mockMvc.perform(post("/gate/barcode/timeout")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRequirerAutenticacao() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("tokenGatePass", "token-xyz");
        request.put("codigoBarcode", "CONT123456");
        request.put("confirmado", true);
        request.put("dispositivoDmtId", "DMT-001");

        mockMvc.perform(post("/gate/barcode/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
