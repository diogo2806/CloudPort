package br.com.cloudport.servicogate.app.auditoria.dto;

import org.springframework.http.MediaType;

public enum FormatoExportacao {
    CSV("text/csv", "csv"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String mediaType;
    private final String extensao;

    FormatoExportacao(String mediaType, String extensao) {
        this.mediaType = mediaType;
        this.extensao = extensao;
    }

    public MediaType getMediaType() {
        return MediaType.parseMediaType(mediaType);
    }

    public String getExtensao() {
        return extensao;
    }

    public static FormatoExportacao from(String value) {
        if (value == null) {
            return CSV;
        }
        return FormatoExportacao.valueOf(value.trim().toUpperCase());
    }
}
