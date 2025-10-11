package br.com.cloudport.servicogate.service;

import org.springframework.core.io.Resource;

public class DocumentoDownload {

    private final Resource resource;
    private final String filename;
    private final String contentType;

    public DocumentoDownload(Resource resource, String filename, String contentType) {
        this.resource = resource;
        this.filename = filename;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
    }

    public Resource getResource() {
        return resource;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }
}
