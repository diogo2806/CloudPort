package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdentidadeMensagemEdiServico {

    public IdentidadeMensagemEdi identificar(TipoMensagemEdi tipo,
                                              String conteudoOriginal,
                                              String referenciaMensagem) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo da mensagem EDI e obrigatorio.");
        }
        if (!StringUtils.hasText(conteudoOriginal)) {
            throw new IllegalArgumentException(tipo + ": conteudo original obrigatorio para recepcao.");
        }

        String hashConteudo = hash(conteudoOriginal);
        String identificadorUnb = extrairElemento(conteudoOriginal, "UNB", 5);
        String identificadorUnh = extrairElemento(conteudoOriginal, "UNH", 1);
        String referenciaNormalizada = normalizar(referenciaMensagem);
        if (!StringUtils.hasText(referenciaNormalizada)) {
            referenciaNormalizada = "HASH-" + hashConteudo.substring(0, 32);
        }

        String materialChave = tipo.name()
                + "|" + valorChave(identificadorUnb, "SEM_UNB")
                + "|" + valorChave(identificadorUnh, "SEM_UNH")
                + "|" + referenciaNormalizada;
        return new IdentidadeMensagemEdi(
                identificadorUnb,
                identificadorUnh,
                referenciaNormalizada,
                hash(materialChave),
                hashConteudo
        );
    }

    private String extrairElemento(String conteudo, String segmentoEsperado, int indice) {
        String normalizado = conteudo.replace('\r', '\n');
        String[] segmentos = normalizado.split("'");
        for (String segmento : segmentos) {
            String limpo = segmento.replace("\n", "").trim();
            if (!limpo.startsWith(segmentoEsperado + "+")) {
                continue;
            }
            String[] elementos = limpo.split("\\+", -1);
            if (elementos.length > indice && StringUtils.hasText(elementos[indice])) {
                return limitar(elementos[indice].trim(), 100);
            }
        }
        return null;
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] calculado = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(calculado);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel no runtime Java.", ex);
        }
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? limitar(valor.trim(), 100) : null;
    }

    private String valorChave(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor : padrao;
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }

    public record IdentidadeMensagemEdi(
            String identificadorUnb,
            String identificadorUnh,
            String referenciaMensagem,
            String chaveIdempotencia,
            String hashConteudo
    ) {
    }
}
