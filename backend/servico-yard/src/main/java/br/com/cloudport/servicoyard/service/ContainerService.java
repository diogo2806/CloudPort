package br.com.cloudport.servicoyard.service;

import br.com.cloudport.servicoyard.model.Container;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ContainerService {
    private final List<Container> containers = new ArrayList<>();
    private final AtomicLong counter = new AtomicLong();

    public List<Container> listContainers() {
        return Collections.unmodifiableList(containers);
    }

    public Container addContainer(Container container) {
        container.setId(counter.incrementAndGet());
        containers.add(container);
        return container;
    }
}
