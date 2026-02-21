package org.obeci.platform.controllers;

import org.obeci.platform.entities.Escola;
import org.obeci.platform.dtos.EscolaCreateRequest;
import org.obeci.platform.dtos.EscolaUpdateRequest;
import jakarta.validation.Valid;
import org.obeci.platform.services.EscolaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/escolas")
/**
 * Controller REST para gestão de escolas.
 *
 * <p>Expõe operações CRUD e consultas simples para {@link Escola}.</p>
 *
 * <p>Dependências:
 * <ul>
 *   <li>{@link EscolaService} (regras de negócio/persistência).</li>
 * </ul>
 * </p>
 */
public class EscolaController {

    @Autowired
    private EscolaService escolaService;

    @GetMapping
    /**
     * Lista todas as escolas.
     */
    public ResponseEntity<List<Escola>> getAllEscolas() {
        List<Escola> escolas = escolaService.getAllEscolas();
        return ResponseEntity.ok(escolas);
    }

    @GetMapping("/{id}")
    /**
     * Obtém escola por id.
     *
     * <p>Saída: {@code Optional<Escola>} (o controller não transforma ausência em 404).</p>
     */
    public ResponseEntity<Optional<Escola>> getEscolaById(@PathVariable Long id) {
        Optional<Escola> escola = escolaService.getEscolaById(id);
        return ResponseEntity.ok(escola);
    }

    @PostMapping
    /**
     * Cria uma escola.
     *
     * <p>Entrada: {@link EscolaCreateRequest}.</p>
     * <p>Saída: {@link Escola} criada.</p>
     *
     * <p>Observação importante: o DTO possui {@code cidade}, mas o campo não é persistido atualmente.</p>
     */
    public ResponseEntity<Escola> createEscola(@Valid @RequestBody EscolaCreateRequest request) {
        Escola escola = new Escola();
        escola.setNome(request.getNome());
        escola.setIsActive(request.getIsActive());
        // request.getCidade() presente no DTO, não persistido atualmente
        Escola createdEscola = escolaService.createEscola(escola);
        return ResponseEntity.ok(createdEscola);
    }

    @PutMapping("/{id}")
    /**
     * Atualiza uma escola por id.
     *
     * <p>Saída: 200 com entidade atualizada; 404 se não encontrada.</p>
     */
    public ResponseEntity<Escola> updateEscola(@PathVariable Long id, @Valid @RequestBody EscolaUpdateRequest request) {
        Escola escolaDetails = new Escola();
        escolaDetails.setNome(request.getNome());
        escolaDetails.setIsActive(request.getIsActive());
        Escola updatedEscola = escolaService.updateEscola(id, escolaDetails);
        if (updatedEscola != null) {
            return ResponseEntity.ok(updatedEscola);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    /**
     * Remove uma escola por id.
     *
     * <p>Saída: 200 {@code true} se removida; 404 se não encontrada.</p>
     */
    public ResponseEntity<Boolean> deleteEscola(@PathVariable Long id) {
        boolean deleted = escolaService.deleteEscola(id);
        if (deleted) {
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/ativo/{isActive}")
    /**
     * Lista escolas filtrando pelo status {@code isActive}.</p>
     */
    public ResponseEntity<List<Escola>> getActiveEscolas(@PathVariable Boolean isActive) {
        List<Escola> escolas = escolaService.getActiveEscolas(isActive);
        return ResponseEntity.ok(escolas);
    }

    @GetMapping("/nome/{nome}")
    /**
     * Busca escolas cujo nome contenha o valor informado.</p>
     */
    public ResponseEntity<List<Escola>> getEscolasByNomeContaining(@PathVariable String nome) {
        List<Escola> escolas = escolaService.getEscolasByNomeContaining(nome);
        return ResponseEntity.ok(escolas);
    }
}