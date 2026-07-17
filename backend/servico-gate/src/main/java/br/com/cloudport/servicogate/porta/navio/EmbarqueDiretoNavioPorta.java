package br.com.cloudport.servicogate.porta.navio;

import java.time.LocalDateTime;

/**
 * Porta usada pelo Gate para confirmar um embarque no plano de estiva sem criar
 * qualquer posição ou movimentação intermediária no pátio.
 */
public interface EmbarqueDiretoNavioPorta {

    Resultado embarcar(Comando comando);

    final class Comando {
        private final Long atribuicaoEstivaId;
        private final String codigoConteiner;
        private final LocalDateTime embarcadoEm;

        public Comando(Long atribuicaoEstivaId, String codigoConteiner, LocalDateTime embarcadoEm) {
            this.atribuicaoEstivaId = atribuicaoEstivaId;
            this.codigoConteiner = codigoConteiner;
            this.embarcadoEm = embarcadoEm;
        }

        public Long getAtribuicaoEstivaId() { return atribuicaoEstivaId; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
    }

    final class Resultado {
        private final Long atribuicaoEstivaId;
        private final Long planoEstivaId;
        private final String codigoConteiner;
        private final int baia;
        private final int fileira;
        private final int camada;
        private final LocalDateTime embarcadoEm;

        public Resultado(Long atribuicaoEstivaId,
                         Long planoEstivaId,
                         String codigoConteiner,
                         int baia,
                         int fileira,
                         int camada,
                         LocalDateTime embarcadoEm) {
            this.atribuicaoEstivaId = atribuicaoEstivaId;
            this.planoEstivaId = planoEstivaId;
            this.codigoConteiner = codigoConteiner;
            this.baia = baia;
            this.fileira = fileira;
            this.camada = camada;
            this.embarcadoEm = embarcadoEm;
        }

        public Long getAtribuicaoEstivaId() { return atribuicaoEstivaId; }
        public Long getPlanoEstivaId() { return planoEstivaId; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public int getBaia() { return baia; }
        public int getFileira() { return fileira; }
        public int getCamada() { return camada; }
        public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
    }
}
