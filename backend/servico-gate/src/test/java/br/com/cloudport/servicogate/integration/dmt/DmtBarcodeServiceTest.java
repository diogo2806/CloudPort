package br.com.cloudport.servicogate.integration.dmt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cloudport.servicogate.config.BarcodeProperties;
import org.junit.jupiter.api.Test;

class DmtBarcodeServiceTest {

    @Test
    void deveIniciarQuandoIntegracaoEstiverDesabilitada() {
        BarcodeProperties properties = new BarcodeProperties();

        assertThatCode(() -> new DmtBarcodeService(properties))
                .doesNotThrowAnyException();
    }

    @Test
    void deveImpedirAtivacaoSemClienteDmtReal() {
        BarcodeProperties properties = new BarcodeProperties();
        properties.setHabilitado(true);

        assertThatThrownBy(() -> new DmtBarcodeService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cliente DMT real");
    }
}
