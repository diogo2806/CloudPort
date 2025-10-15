package br.com.cloudport.servicoyard.app.gestor;

import br.com.cloudport.servicoyard.model.Container;
import br.com.cloudport.servicoyard.app.gestor.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/yard/containers")
public class ContainerController {
    @Autowired
    private ContainerService containerService;

    @GetMapping
    public List<Container> list() {
        return containerService.listContainers();
    }

    @PostMapping
    public ResponseEntity<Container> add(@RequestBody Container container) {
        Container saved = containerService.addContainer(container);
        return ResponseEntity.ok(saved);
    }
}
