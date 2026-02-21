package org.obeci.platform.dtos.collab;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Mensagem recebida via WebSocket (STOMP) para atualizar um instrumento.
 *
 * <p>Importante: esta mensagem representa uma "intenção" do cliente.
 * O servidor é a fonte de verdade: ele valida, persiste, incrementa versão e então
 * faz broadcast do estado atualizado para todos os participantes.</p>
 */
@Data
public class InstrumentoWsUpdateRequest {

    /** turmaId do instrumento sendo editado */
    private Long turmaId;

    /** Snapshot do documento/slides (estrutura livre do front-end). */
    private JsonNode slides;

    /**
     * Versão que o cliente acredita ser a atual.
     *
     * <p>Se não bater com a versão do banco, o servidor retorna conflito para evitar
     * sobrescrita silenciosa.</p>
     */
    private Long expectedVersion;

    /**
     * Identificador do cliente (UUID no front) para evitar eco/loop:
     * o cliente ignora eventos que ele mesmo originou.
     */
    private String clientId;

    /**
     * Resumo legível para o log (ex.: "Editou texto no Slide 2").
     * Deve ser curto.
     */
    private String summary;

    /**
     * Tipo do evento (ex.: SNAPSHOT_UPDATE). Útil para filtrar o log no futuro.
     */
    private String eventType;
}
