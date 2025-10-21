package br.com.cloudport.servicorail.ferrovia.controlador;

import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicorail.ferrovia.servico.ImportacaoManifestoVisitaServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rail/ferrovia/visitas/importacoes")
public class ImportacaoManifestoVisitaControlador {

    private final ImportacaoManifestoVisitaServico importacaoManifestoVisitaServico;

    public ImportacaoManifestoVisitaControlador(ImportacaoManifestoVisitaServico importacaoManifestoVisitaServico) {
        this.importacaoManifestoVisitaServico = importacaoManifestoVisitaServico;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public VisitaTremRespostaDto importar(@RequestPart("arquivo") MultipartFile arquivo) {
        return importacaoManifestoVisitaServico.importarManifesto(arquivo);
    }
}
