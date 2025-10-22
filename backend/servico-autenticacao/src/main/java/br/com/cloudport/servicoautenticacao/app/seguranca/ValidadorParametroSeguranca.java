package br.com.cloudport.servicoautenticacao.app.seguranca;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ValidadorParametroSeguranca {

    private static final Pattern PADRAO_INJECAO = Pattern.compile("(?i)(<|>|script|onerror|onload|onfocus|onmouseover|onmouseenter|javascript:|alert\\(|\\"|\\'|`)");

    public void validarParametroOpcional(String valor, String nomeParametro) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (PADRAO_INJECAO.matcher(valor).find()) {
            throw new ParametroSegurancaInvalidoException(
                    String.format("Parâmetro %s contém caracteres não permitidos.", nomeParametro)
            );
        }
    }
}
