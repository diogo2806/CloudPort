package br.com.cloudport.servicoyard.vesselplanner.modelo;

import java.util.Locale;

public final class ComprimentoConteiner {

    private ComprimentoConteiner() {
    }

    public static int exigirComprimentoPes(String isoCode) {
        String normalizado = normalizar(isoCode);
        if (normalizado.isEmpty()) {
            throw new IllegalStateException("ISO code ausente; não é possível validar o comprimento do contêiner");
        }

        char codigoComprimento = normalizado.charAt(0);
        if (codigoComprimento == '2') {
            return 20;
        }
        if (codigoComprimento == '4') {
            return 40;
        }
        if (codigoComprimento == 'L') {
            return 45;
        }

        throw new IllegalStateException(
                "Comprimento do contêiner não suportado pelo perfil: ISO code " + normalizado);
    }

    public static boolean isReefer(String isoCode) {
        String normalizado = normalizar(isoCode);
        return normalizado.length() >= 3 && normalizado.charAt(2) == 'R';
    }

    private static String normalizar(String isoCode) {
        return isoCode == null ? "" : isoCode.trim().toUpperCase(Locale.ROOT);
    }
}
