package br.com.cloudport.servicogate.config;

import br.com.cloudport.servicogate.app.gestor.GateOperationsController;
import br.com.cloudport.servicogate.model.enums.GateCallPriority;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GateCallRequestCompatibilityConfig {

    @Bean
    public Module gateCallRequestCompatibilityModule() {
        SimpleModule module = new SimpleModule("gate-call-request-compatibility");
        module.addDeserializer(GateOperationsController.CallRequest.class, new CallRequestDeserializer());
        return module;
    }

    private static class CallRequestDeserializer extends JsonDeserializer<GateOperationsController.CallRequest> {
        @Override
        public GateOperationsController.CallRequest deserialize(JsonParser parser,
                                                                 DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            GateOperationsController.CallRequest request = new GateOperationsController.CallRequest();
            if (node.hasNonNull("gatePassId")) {
                request.setGatePassId(node.get("gatePassId").longValue());
            }
            if (node.hasNonNull("prioridade")) {
                request.setPrioridade(GateCallPriority.valueOf(node.get("prioridade").asText().toUpperCase()));
            }
            request.setGatePista(node.hasNonNull("gatePista")
                    ? node.get("gatePista").asText()
                    : "PATIO");
            request.setValidadeMinutos(node.hasNonNull("validadeMinutos")
                    ? node.get("validadeMinutos").intValue()
                    : 5);
            if (node.hasNonNull("operador")) {
                request.setOperador(node.get("operador").asText());
            }
            return request;
        }
    }
}
