package org.obeci.platform.controllers;

import jakarta.persistence.OptimisticLockException;
import org.obeci.platform.dtos.collab.InstrumentoWsUpdateBroadcast;
import org.obeci.platform.dtos.collab.InstrumentoWsUpdateRequest;
import org.obeci.platform.services.InstrumentoCollaborationService;
import org.obeci.platform.services.InstrumentoAccessService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Controller de mensagens WebSocket (STOMP) para colaboração do Instrumento.
 *
 * <h2>Destinos</h2>
 * <ul>
 *   <li>Cliente envia update para: <b>/app/instrumentos/update</b></li>
 *   <li>Servidor faz broadcast do estado para: <b>/topic/instrumentos/{turmaId}</b></li>
 *   <li>Servidor envia erros para: <b>/user/queue/instrumentos/errors</b></li>
 * </ul>
 *
 * <h2>Segurança</h2>
 * <p>Este endpoint depende do usuário autenticado associado à sessão STOMP.
 * Se {@link Principal} for null, respondemos erro e não aplicamos alterações.</p>
 */
@Controller
public class InstrumentoWsController {

    private final InstrumentoCollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final InstrumentoAccessService instrumentoAccessService;

    public InstrumentoWsController(
            InstrumentoCollaborationService collaborationService,
            SimpMessagingTemplate messagingTemplate,
            InstrumentoAccessService instrumentoAccessService
    ) {
        this.collaborationService = collaborationService;
        this.messagingTemplate = messagingTemplate;
        this.instrumentoAccessService = instrumentoAccessService;
    }

    @MessageMapping("/instrumentos/update")
    public void updateInstrumento(InstrumentoWsUpdateRequest req, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // Sem user na sessão STOMP -> não permitir update.
            messagingTemplate.convertAndSendToUser(
                    "anonymous",
                    "/queue/instrumentos/errors",
                    new WsError("UNAUTHENTICATED", "Sessão não autenticada", null, req == null ? null : req.getClientId())
            );
            return;
        }

        // Só ADMIN ou professor pertencente à turma pode publicar updates.
        if (req != null) {
            try {
                instrumentoAccessService.assertCanAccessTurmaInstrumento(req.getTurmaId(), authentication);
            } catch (Exception e) {
                messagingTemplate.convertAndSendToUser(
                        authentication.getName(),
                        "/queue/instrumentos/errors",
                        new WsError("FORBIDDEN", "Sem permissão para acessar este instrumento", req.getTurmaId(), req.getClientId())
                );
                return;
            }
        }

        String actor = authentication.getName();

        try {
            InstrumentoWsUpdateBroadcast broadcast = collaborationService.applySnapshotUpdate(
                    req.getTurmaId(),
                    req.getSlides(),
                    req.getExpectedVersion(),
                    actor,
                    req.getClientId(),
                    req.getEventType(),
                    req.getSummary()
            );

            // Nada a retornar: o broadcast já foi enviado para /topic.
        } catch (OptimisticLockException e) {
            // Conflito de versão: o cliente precisa ressincronizar.
            messagingTemplate.convertAndSendToUser(
                    actor,
                    "/queue/instrumentos/errors",
                    new WsError("VERSION_CONFLICT", e.getMessage(), req.getTurmaId(), req.getClientId())
            );
        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(
                    actor,
                    "/queue/instrumentos/errors",
                    new WsError("UPDATE_FAILED", e.getMessage(), req.getTurmaId(), req.getClientId())
            );
        }
    }

    /**
     * Estrutura de erro enviada ao usuário em /user/queue/instrumentos/errors.
     *
     * <p>O front usa isso para exibir aviso e fazer resync quando necessário.</p>
     */
    public record WsError(String code, String message, Long turmaId, String clientId, LocalDateTime at) {
        public WsError(String code, String message, Long turmaId, String clientId) {
            this(code, message, turmaId, clientId, LocalDateTime.now());
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception e, Principal principal) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/instrumentos/errors",
                new WsError("UNHANDLED", e.getMessage(), null, null)
        );
    }
}
