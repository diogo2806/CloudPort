package br.com.cloudport.servicogate.storage;

public class StoredDocumento {

    private final String storageKey;
    private final String nomeOriginal;
    private final String contentType;
    private final long tamanho;

    public StoredDocumento(String storageKey, String nomeOriginal, String contentType, long tamanho) {
        this.storageKey = storageKey;
        this.nomeOriginal = nomeOriginal;
        this.contentType = contentType;
        this.tamanho = tamanho;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public long getTamanho() {
        return tamanho;
    }
}
