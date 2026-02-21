package org.obeci.platform.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.obeci.platform.entities.Instrumento;
import org.obeci.platform.entities.InstrumentoImage;
import org.obeci.platform.repositories.InstrumentoImageRepository;
import org.obeci.platform.repositories.InstrumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
/**
 * Camada de serviço para Instrumento (slides) e imagens associadas.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Buscar/criar instrumento por turma.</li>
 *   <li>Persistir JSON de slides (string) a partir de {@link JsonNode}.</li>
 *   <li>Armazenar e recuperar imagens (bytes) via {@link InstrumentoImage}.</li>
 * </ul>
 * </p>
 *
 * <p>Pontos críticos:
 * <ul>
 *   <li>{@link #defaultSlidesJson()} define o formato inicial esperado pelo editor do front-end.</li>
 *   <li>As operações de save executam escrita no banco e podem lançar {@link IOException} ao serializar JSON.</li>
 * </ul>
 * </p>
 */
public class InstrumentoService {

    @Autowired
    private InstrumentoRepository instrumentoRepository;

    @Autowired
    private InstrumentoImageRepository imageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    /**
     * Busca instrumento por id da turma.</p>
     */
    public Optional<Instrumento> getByTurmaId(Long turmaId) {
        return instrumentoRepository.findByTurmaId(turmaId);
    }

    @Transactional
    /**
     * Cria um instrumento vazio para a turma, se ainda não existir.
     *
     * <p>Efeito colateral: pode inserir novo registro com JSON default.</p>
     */
    public Instrumento createEmptyForTurma(Long turmaId) {
        if (instrumentoRepository.existsByTurmaId(turmaId)) {
            return instrumentoRepository.findByTurmaId(turmaId).get();
        }
        Instrumento instrumento = new Instrumento();
        instrumento.setTurmaId(turmaId);
        instrumento.setSlidesJson(defaultSlidesJson());
        return instrumentoRepository.save(instrumento);
    }

    @Transactional
    /**
     * Persiste os slides (JSON) do instrumento da turma.
     *
     * <p>Entrada: {@link JsonNode} (estrutura livre vinda do front-end).</p>
     * <p>Saída: entidade {@link Instrumento} persistida.</p>
     */
    public Instrumento saveSlides(Long turmaId, JsonNode slidesNode) throws IOException {
        // Regra: não criar instrumento implicitamente a partir de um turmaId arbitrário.
        // O instrumento deve ser criado no fluxo de criação de turma.
        Instrumento instrumento = instrumentoRepository.findByTurmaId(turmaId)
            .orElseThrow(() -> new IllegalStateException("Instrumento não encontrado para a turmaId=" + turmaId));
        String json = objectMapper.writeValueAsString(slidesNode);
        instrumento.setSlidesJson(json);
        return instrumentoRepository.save(instrumento);
    }

    public InstrumentoImage saveImage(MultipartFile file) throws IOException {
        // Efeito colateral: persiste bytes da imagem no banco.
        InstrumentoImage img = new InstrumentoImage();
        img.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        img.setOriginalName(file.getOriginalFilename());
        img.setData(file.getBytes());
        return imageRepository.save(img);
    }

    public Optional<InstrumentoImage> getImage(Long id) {
        return imageRepository.findById(id);
    }

    private String defaultSlidesJson() {
        // Cria dois slides iniciais vazios compatíveis com o editor.
        // Observação: os campos e defaults aqui precisam ficar alinhados ao contrato implícito do front-end.
        ArrayNode arr = objectMapper.createArrayNode();

        ObjectNode slide1 = objectMapper.createObjectNode();
        slide1.put("id", 1);
        slide1.put("content", "");
        ObjectNode styles1 = objectMapper.createObjectNode();
        styles1.put("fontSize", "24px");
        styles1.put("fontWeight", "normal");
        styles1.put("fontStyle", "normal");
        styles1.put("textDecoration", "none");
        styles1.put("fontFamily", "Nunito");
        slide1.set("styles", styles1);
        slide1.set("textBoxes", objectMapper.createArrayNode());
        slide1.set("images", objectMapper.createArrayNode());
        slide1.putNull("instrument");
        slide1.set("tags", objectMapper.createArrayNode());
        arr.add(slide1);

        ObjectNode slide2 = objectMapper.createObjectNode();
        slide2.put("id", 2);
        slide2.put("content", "");
        ObjectNode styles2 = objectMapper.createObjectNode();
        styles2.put("fontSize", "24px");
        styles2.put("fontWeight", "normal");
        styles2.put("fontStyle", "normal");
        styles2.put("textDecoration", "none");
        styles2.put("fontFamily", "Nunito");
        slide2.set("styles", styles2);
        slide2.set("textBoxes", objectMapper.createArrayNode());
        slide2.set("images", objectMapper.createArrayNode());
        slide2.putNull("instrument");
        slide2.set("tags", objectMapper.createArrayNode());
        arr.add(slide2);

        return arr.toString();
    }
}
