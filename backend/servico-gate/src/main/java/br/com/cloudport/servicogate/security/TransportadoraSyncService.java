package br.com.cloudport.servicogate.security;

import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.repository.TransportadoraRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TransportadoraSyncService {

    private final TransportadoraRepository transportadoraRepository;

    public TransportadoraSyncService(TransportadoraRepository transportadoraRepository) {
        this.transportadoraRepository = transportadoraRepository;
    }

    @Transactional
    public void sincronizarTransportadora(String documento, String nome) {
        if (!StringUtils.hasText(documento)) {
            return;
        }

        String normalizedDocumento = documento.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        Optional<Transportadora> existente = transportadoraRepository.findByDocumento(normalizedDocumento);
        if (existente.isPresent()) {
            Transportadora transportadora = existente.get();
            if (StringUtils.hasText(nome) && !nome.equals(transportadora.getNome())) {
                transportadora.setNome(nome);
                transportadoraRepository.save(transportadora);
            }
            return;
        }

        Transportadora nova = new Transportadora();
        nova.setDocumento(normalizedDocumento);
        nova.setNome(StringUtils.hasText(nome) ? nome : normalizedDocumento);
        transportadoraRepository.save(nova);
    }
}
