package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlotNavioDto {

    private Long id;
    private int bay;
    private int rowBay;
    private int tier;
    private String tipoSlot;
    private String codigoHatchCover;
    private boolean sobreHatchCover;
    private boolean restrito;
    private String motivoRestricao;
    private boolean tomadaReefer;
    private boolean aceita20Pes;
    private boolean aceita40Pes;
    private boolean aceita45Pes;
    private Double maxPesoKg;
    private Double maxPesoPilhaKg;
    private Double posLongitudinalMetros;
    private Double posTransversalMetros;
    private Double posVerticalMetros;
    private String codigoContainer;
    private String isoCode;
    private Double pesoKg;
    private Double pesoVgmKg;
    private EstadoCargaContainer estadoCarga;
    private String portoCarga;
    private String portoDescarga;
    private String classeImo;
    private String numeroOnu;
    private String grupoSegregacao;
    private boolean perigoso;
    private boolean reefer;
    private Double temperaturaRequeridaC;
    private Double temperaturaMinimaC;
    private Double temperaturaMaximaC;
    private boolean oog;
    private Double excessoFrontalCm;
    private Double excessoTraseiroCm;
    private Double excessoEsquerdoCm;
    private Double excessoDireitoCm;
    private Double excessoAlturaCm;
    private String statusAlertas;
}
