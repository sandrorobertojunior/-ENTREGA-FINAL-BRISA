package org.obeci.platform.configs;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.obeci.platform.services.InstrumentoAccessService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor do canal STOMP (mensagens inbound do cliente).
 *
 * <p>Função principal: garantir que a sessão STOMP tenha um "user" (Principal)
 * associado para que:
 * <ul>
 *   <li>o controller WS possa receber {@link java.security.Principal}</li>
 *   <li>o servidor possa enviar mensagens para /user/queue/**</li>
 * </ul>
 * </p>
 *
 * <p>Fonte do Authentication:
 * <ul>
 *   <li>salvo no handshake em {@link JwtHandshakeInterceptor#ATTR_AUTH}</li>
 * </ul>
 * </p>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_INSTRUMENTO_TURMA = Pattern.compile("^/topic/instrumentos/(\\d+)$");

    private final InstrumentoAccessService instrumentoAccessService;

    public StompAuthChannelInterceptor(InstrumentoAccessService instrumentoAccessService) {
        this.instrumentoAccessService = instrumentoAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // No CONNECT, copiamos Authentication do handshake para o accessor.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication auth = resolveAuthFromSession(accessor);
            if (auth != null) {
                accessor.setUser(auth);
            }
        }

        // No SUBSCRIBE, bloqueamos assinatura de tópicos por turma se o usuário não pertence.
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null) {
                Matcher m = TOPIC_INSTRUMENTO_TURMA.matcher(destination);
                if (m.matches()) {
                    Long turmaId = Long.parseLong(m.group(1));
                    Authentication auth = null;
                    if (accessor.getUser() instanceof Authentication a) {
                        auth = a;
                    } else {
                        auth = resolveAuthFromSession(accessor);
                    }
                    instrumentoAccessService.assertCanAccessTurmaInstrumento(turmaId, auth);
                }
            }
        }

        return message;
    }

    @Nullable
    private Authentication resolveAuthFromSession(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) {
            return null;
        }

        Object a = accessor.getSessionAttributes().get(JwtHandshakeInterceptor.ATTR_AUTH);
        if (a instanceof Authentication auth) {
            return auth;
        }
        return null;
    }
}
