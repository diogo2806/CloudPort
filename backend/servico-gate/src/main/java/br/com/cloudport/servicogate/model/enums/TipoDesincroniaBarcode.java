package br.com.cloudport.servicogate.model.enums;

public enum TipoDesincroniaBarcode {
    CONTAINER_PRESO("Container entrou mas nunca saiu"),
    BARCODE_NAO_CONFIRMADO("Container liberado sem confirmação de barcode"),
    BARCODE_MISMATCH("Barcode confirmado não corresponde ao esperado"),
    TIMEOUT_NAO_RESOLVIDO("Timeout de barcode nunca foi resolvido"),
    STATUS_INCONSISTENTE("Status de barcode inconsistente com TOS"),
    ENTRADA_SEM_SAIDA_24H("Container na entrada há mais de 24 horas"),
    DISCREPANCIA_TEMPORAL("Timestamps inconsistentes (entrada > saída)");

    private final String descricao;

    TipoDesincroniaBarcode(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
