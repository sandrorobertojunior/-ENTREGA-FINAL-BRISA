package org.obeci.platform.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração de WebSocket + STOMP para colaboração em tempo real.
 *
 * <h2>Como funciona (visão geral)</h2>
 * <ul>
 *   <li>O navegador abre um WebSocket em <b>/ws</b>.</li>
 *   <li>Sobre o WebSocket, usamos o protocolo <b>STOMP</b> para mensagens.</li>
 *   <li>O cliente envia comandos para destinos <b>/app/**</b> (entrada do servidor).</li>
 *   <li>O servidor publica eventos para destinos <b>/topic/**</b> (broadcast).</li>
 *   <li>Erros/respostas específicas do usuário usam <b>/user/queue/**</b>.</li>
 * </ul>
 *
 * <h2>Segurança</h2>
 * <p>O handshake do WebSocket é um HTTP GET normal para /ws. Isso significa que:
 * <ul>
 *   <li>o mesmo filtro JWT usado nas APIs REST pode autenticar o handshake</li>
 *   <li>cookies HttpOnly (token) são enviados automaticamente quando o front e o back
 *       estão no mesmo host (ex.: localhost)</li>
 * </ul>
 * </p>
 *
 * <p>Além disso, registramos um interceptor de handshake e um interceptor de canal
 * para garantir que o usuário autenticado seja associado à sessão STOMP.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final AppCorsProperties corsProperties;

    public WebSocketConfig(
            JwtHandshakeInterceptor jwtHandshakeInterceptor,
            StompAuthChannelInterceptor stompAuthChannelInterceptor,
            AppCorsProperties corsProperties
    ) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint do handshake WebSocket.
        // allowedOriginPatterns precisa ser compatível com allowCredentials(true).
        String[] allowed = corsProperties.getAllowedOrigins() == null
            ? new String[0]
            : corsProperties.getAllowedOrigins().toArray(new String[0]);

        registry.addEndpoint("/ws")
            // Reusa exatamente as origens configuradas em app.cors.allowed-origins
            // (application-dev.yml, application-prod.yml, etc.).
            .setAllowedOriginPatterns(allowed)
            .addInterceptors(jwtHandshakeInterceptor);

        // Nota: não habilitamos SockJS aqui para manter o fluxo simples e evitar
        // camadas extras (e CORS adicional). Se precisar suportar browsers antigos,
        // podemos adicionar .withSockJS() futuramente.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Mensagens enviadas pelo cliente para /app/** serão roteadas para @MessageMapping.
        registry.setApplicationDestinationPrefixes("/app");

        // Broker simples em memória (suficiente para DEV e para esta fase).
        // Se um dia escalar para múltiplas instâncias, migrar para broker externo (RabbitMQ/Redis).
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefixo padrão para destinos de usuário: /user/queue/**
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Intercepta mensagens STOMP vindas do cliente para associar Authentication à sessão.
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
