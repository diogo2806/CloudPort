package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.ValidacaoPlanoBulkDto;

public class ValidacaoPlanoBulkException extends IllegalStateException {

    private final ValidacaoPlanoBulkDto validacao;

    public ValidacaoPlanoBulkException(String mensagem, ValidacaoPlanoBulkDto validacao) {
        super(mensagem);
        this.validacao = validacao;
    }

    public ValidacaoPlanoBulkDto getValidacao() {
        return validacao;
    }
}
