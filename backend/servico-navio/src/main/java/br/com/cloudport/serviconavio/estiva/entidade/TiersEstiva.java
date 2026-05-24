package br.com.cloudport.serviconavio.estiva.entidade;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras de numeração dos tiers no padrão Bay-Row-Tier:
 * porão usa tiers pares a partir de 02 e o convés a partir de 82,
 * o que permite derivar automaticamente a localização (PORAO/CONVES).
 */
public final class TiersEstiva {

    public static final int PASSO = 2;
    public static final int BASE_PORAO = 2;
    public static final int BASE_CONVES = 82;
    public static final int LIMITE_CONVES = 80;

    private TiersEstiva() {
    }

    public static List<Integer> tiersPorao(int camadasPorao) {
        List<Integer> tiers = new ArrayList<>();
        for (int i = 0; i < camadasPorao; i++) {
            tiers.add(BASE_PORAO + i * PASSO);
        }
        return tiers;
    }

    public static List<Integer> tiersConves(int camadasConves) {
        List<Integer> tiers = new ArrayList<>();
        for (int i = 0; i < camadasConves; i++) {
            tiers.add(BASE_CONVES + i * PASSO);
        }
        return tiers;
    }

    public static ConvesNavio convesDoTier(int tier) {
        return tier >= LIMITE_CONVES ? ConvesNavio.CONVES : ConvesNavio.PORAO;
    }

    public static boolean tierValido(int tier, int camadasPorao, int camadasConves) {
        return tiersPorao(camadasPorao).contains(tier) || tiersConves(camadasConves).contains(tier);
    }
}
