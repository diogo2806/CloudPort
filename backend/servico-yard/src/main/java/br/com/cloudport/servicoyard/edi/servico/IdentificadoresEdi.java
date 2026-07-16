package br.com.cloudport.servicoyard.edi.servico;

public record IdentificadoresEdi(
        String interchangeControlReference,
        String messageReferenceNumber
) {
}
