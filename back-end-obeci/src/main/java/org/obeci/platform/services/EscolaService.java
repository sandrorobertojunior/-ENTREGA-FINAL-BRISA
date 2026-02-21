package org.obeci.platform.services;

import org.obeci.platform.entities.Escola;
import org.obeci.platform.repositories.EscolaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
/**
 * Camada de serviço para escolas.
 *
 * <p>Encapsula operações de persistência e consultas básicas para {@link Escola}.</p>
 */
public class EscolaService {

    @Autowired
    private EscolaRepository escolaRepository;

    public List<Escola> getAllEscolas() {
        return escolaRepository.findAll();
    }

    public Optional<Escola> getEscolaById(Long id) {
        return escolaRepository.findById(id);
    }

    public Escola createEscola(Escola escola) {
        return escolaRepository.save(escola);
    }

    public Escola updateEscola(Long id, Escola escolaDetails) {
        Escola escola = escolaRepository.findById(id).orElse(null);
        if (escola != null) {
            escola.setNome(escolaDetails.getNome());
            escola.setIsActive(escolaDetails.getIsActive());
            return escolaRepository.save(escola);
        }
        return null;
    }

    public boolean deleteEscola(Long id) {
        if (escolaRepository.existsById(id)) {
            escolaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Escola> getActiveEscolas(Boolean isActive) {
        return escolaRepository.findByIsActive(isActive);
    }

    public List<Escola> getEscolasByNomeContaining(String nome) {
        return escolaRepository.findByNomeContainingIgnoreCase(nome);
    }
}