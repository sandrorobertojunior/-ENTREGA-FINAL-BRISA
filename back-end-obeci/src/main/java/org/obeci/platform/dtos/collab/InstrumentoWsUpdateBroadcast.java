package org.obeci.platform.dtos.collab;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mensagem enviada via WebSocket (STOMP) para todos os participantes da turma.
 *
 * <p>Essa é a mensagem que torna a edição "tempo real": qualquer cliente inscrito
 * no tópico do instrumento recebe o estado atualizado e aplica na UI.</p>
 */
@Data
@AllArgsConstructor
public class InstrumentoWsUpdateBroadcast {

    private Long instrumentoId;
    private Long turmaId;

    /** Snapshot do documento (estrutura livre do front). */
    private JsonNode slides;

    /** Versão do documento após persistência no servidor. */
    private Long version;

    /** Quem efetuou a alteração (username/email do token). */
    private String updatedBy;

    /** Quando o servidor persistiu a alteração. */
    private LocalDateTime updatedAt;

    /** clientId originador (para evitar eco). */
    private String clientId;

    /** Entrada já pronta para o painel de log em tempo real. */
    private InstrumentoChangeLogDto changeLog;
}
