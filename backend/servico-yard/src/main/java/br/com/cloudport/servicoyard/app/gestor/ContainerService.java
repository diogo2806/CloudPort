package br.com.cloudport.servicoyard.app.gestor;

import br.com.cloudport.servicoyard.model.Container;
import br.com.cloudport.servicoyard.app.gestor.ContainerRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContainerService {
    private final ContainerRepository containerRepository;

    public ContainerService(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Transactional(readOnly = true)
    public List<Container> listContainers() {
        return containerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Container addContainer(Container container) {
        container.setId(null);
        return containerRepository.save(container);
    }
}
