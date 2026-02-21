package org.obeci.platform.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.obeci.platform.dtos.collab.InstrumentoChangeLogDto;
import org.obeci.platform.dtos.collab.InstrumentoWsUpdateBroadcast;
import org.obeci.platform.entities.Instrumento;
import org.obeci.platform.entities.InstrumentoChangeLog;
import org.obeci.platform.repositories.InstrumentoChangeLogRepository;
import org.obeci.platform.repositories.InstrumentoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço responsável por orquestrar colaboração em tempo real do Instrumento.
 *
 * <p>Este serviço foi separado do {@link InstrumentoService} para não quebrar o que já funciona:
 * o fluxo REST atual continua existindo, e o modo colaborativo usa este serviço para:
 * <ul>
 *   <li>validar concorrência (version/optimistic locking)</li>
 *   <li>persistir o snapshot do documento</li>
 *   <li>registrar log de alterações</li>
 *   <li>broadcast para participantes via WebSocket</li>
 * </ul>
 * </p>
 */
@Service
public class InstrumentoCollaborationService {

    private final InstrumentoRepository instrumentoRepository;
    private final InstrumentoChangeLogRepository changeLogRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public InstrumentoCollaborationService(
            InstrumentoRepository instrumentoRepository,
            InstrumentoChangeLogRepository changeLogRepository,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.instrumentoRepository = instrumentoRepository;
        this.changeLogRepository = changeLogRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Aplica uma atualização (snapshot) no instrumento e faz broadcast em seguida.
     *
     * @param turmaId turma alvo
     * @param slidesNode snapshot (estrutura livre)
     * @param expectedVersion versão que o cliente acredita estar atual
     * @param actor username/email do autor
     * @param clientId id do cliente originador (para evitar eco)
     * @param eventType tipo do evento (ex.: SNAPSHOT_UPDATE)
     * @param summary resumo legível para o log
     */
    @Transactional
    public InstrumentoWsUpdateBroadcast applySnapshotUpdate(
            Long turmaId,
            JsonNode slidesNode,
            Long expectedVersion,
            String actor,
            String clientId,
            String eventType,
            String summary
    ) {
        Instrumento instrumento = instrumentoRepository.findByTurmaId(turmaId)
                .orElseThrow(() -> new IllegalStateException("Instrumento não encontrado para turmaId=" + turmaId));

        // Controle de concorrência explícito: se o cliente está desatualizado, não sobrescrevemos.
        if (expectedVersion != null && instrumento.getVersion() != null && !instrumento.getVersion().equals(expectedVersion)) {
            throw new OptimisticLockException("Versão desatualizada. expected=" + expectedVersion + " actual=" + instrumento.getVersion());
        }

        try {
            String json = objectMapper.writeValueAsString(slidesNode);

            // Defesa: se o snapshot recebido é idêntico ao que já está no banco,
            // evitamos gravar novamente e evitamos criar spam no change log.
            // Ainda assim fazemos broadcast como ACK para o cliente concluir o "save".
            if (json != null && json.equals(instrumento.getSlidesJson())) {
                InstrumentoWsUpdateBroadcast broadcast = new InstrumentoWsUpdateBroadcast(
                        instrumento.getId(),
                        instrumento.getTurmaId(),
                        slidesNode,
                        instrumento.getVersion(),
                        actor,
                        LocalDateTime.now(),
                        clientId,
                        null
                );
                messagingTemplate.convertAndSend("/topic/instrumentos/" + turmaId, broadcast);
                return broadcast;
            }

            instrumento.setSlidesJson(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao serializar slides", e);
        }

        Instrumento saved = instrumentoRepository.save(instrumento);

        String effectiveEventType = (eventType == null || eventType.isBlank()) ? "SNAPSHOT_UPDATE" : eventType;
        String effectiveSummary = (summary == null || summary.isBlank()) ? "Atualizou o instrumento" : summary;

        InstrumentoChangeLogDto logDto = null;
        if (shouldPersistChangeLog(effectiveEventType, effectiveSummary)) {
            // Log de alterações (humano + auditável)
            InstrumentoChangeLog log = new InstrumentoChangeLog();
            log.setInstrumentoId(saved.getId());
            log.setTurmaId(saved.getTurmaId());
            log.setActor(actor);
            log.setEventType(effectiveEventType);
            log.setSummary(effectiveSummary);

            // Payload reduzido (não o documento inteiro). Útil para depuração e UI.
            try {
                log.setPayloadJson(objectMapper.createObjectNode()
                        .put("clientId", clientId == null ? "" : clientId)
                        .put("version", saved.getVersion() == null ? -1 : saved.getVersion())
                        .toString());
            } catch (Exception ignored) {
                // Não falhar operação por causa do payload do log.
            }

            InstrumentoChangeLog persistedLog = changeLogRepository.save(log);
            logDto = new InstrumentoChangeLogDto(
                    persistedLog.getId(),
                    persistedLog.getInstrumentoId(),
                    persistedLog.getTurmaId(),
                    persistedLog.getActor(),
                    persistedLog.getEventType(),
                    persistedLog.getSummary(),
                    persistedLog.getPayloadJson(),
                    persistedLog.getCreatedAt()
            );
        }

        InstrumentoWsUpdateBroadcast broadcast = new InstrumentoWsUpdateBroadcast(
                saved.getId(),
                saved.getTurmaId(),
                slidesNode,
                saved.getVersion(),
                actor,
                LocalDateTime.now(),
                clientId,
                logDto
        );

        // Broadcast do estado atualizado para todos os clientes da turma.
        messagingTemplate.convertAndSend("/topic/instrumentos/" + turmaId, broadcast);
        // Broadcast também do log (painel de alterações pode assinar esse tópico).
        if (logDto != null) {
            messagingTemplate.convertAndSend("/topic/instrumentos/" + turmaId + "/changes", logDto);
        }

        return broadcast;
    }

    private static boolean shouldPersistChangeLog(String eventType, String summary) {
        if (eventType != null && eventType.toUpperCase().startsWith("INTERNAL_")) {
            return false;
        }
        if (summary == null) return true;
        String s = summary.toLowerCase();
        // Defesa: clientes antigos podem enviar o retry como "summary".
        if (s.contains("retry") && s.contains("conflito")) return false;
        if (s.contains("version_conflict")) return false;
        return true;
    }

    /**
     * Carrega o histórico mais recente de alterações para uma turma.
     */
    @Transactional(readOnly = true)
    public java.util.List<InstrumentoChangeLogDto> getRecentChanges(Long turmaId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return changeLogRepository
                .findByTurmaIdOrderByCreatedAtDesc(turmaId, PageRequest.of(0, safeLimit))
                .stream()
                .map(e -> new InstrumentoChangeLogDto(
                        e.getId(),
                        e.getInstrumentoId(),
                        e.getTurmaId(),
                        e.getActor(),
                        e.getEventType(),
                        e.getSummary(),
                        e.getPayloadJson(),
                        e.getCreatedAt()
                ))
                .toList();
    }
}
