package br.com.cloudport.servicorail.ferrovia.importacao;

public interface ArquivoManifestoVisitaParser {

    boolean suporta(String nomeArquivo, byte[] conteudo);

    ResultadoManifestoVisita parse(String nomeArquivo, byte[] conteudo);
}
