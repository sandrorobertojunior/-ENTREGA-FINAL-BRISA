package org.obeci.platform.configs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.obeci.platform.exceptions.DuplicateTurmaException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
/**
 * Centraliza o tratamento de exceções em controllers REST.
 *
 * <p>Objetivo: padronizar respostas de erro (JSON) e códigos HTTP para validação e
 * exceções de negócio.</p>
 *
 * <p>Formato típico de erro:
 * <ul>
 *   <li>{@code { "error": "mensagem" }}</li>
 *   <li>Para validações: {@code { "error": "Validation failed", "errors": [ {field,message}, ... ] }}</li>
 * </ul>
 * </p>
 */
public class GlobalExceptionHandler {

    // Regra de negócio: duplicidade de turma (mesmo nome dentro da mesma escola).
    // Retorna 409 (Conflict) para diferenciar de validações de DTO (400).
    @ExceptionHandler(DuplicateTurmaException.class)
    /**
     * Trata duplicidade de turma (regra de negócio) como {@code 409 Conflict}.
     */
    public ResponseEntity<Map<String, Object>> handleDuplicateTurma(DuplicateTurmaException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(409).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * Trata falhas de validação de DTO (Bean Validation) como {@code 400 Bad Request}.
     */
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> {
                    Map<String, String> e = new HashMap<>();
                    e.put("field", err.getField());
                    e.put("message", err.getDefaultMessage());
                    return e;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    /**
     * Preserva o status HTTP de {@link ResponseStatusException}.
     *
     * <p>Importante para endpoints que lançam 401/403/404 e não devem ser
     * rebaixados para 400 pelo handler genérico de RuntimeException.</p>
     */
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = ex.getMessage();
        }
        body.put("error", message);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    /**
     * Fallback para {@link RuntimeException} como {@code 400 Bad Request}.
     *
     * <p>Observação: este handler captura uma categoria ampla de erros e pode esconder
     * diferenciações entre erro de cliente vs erro interno. Se houver necessidade futura,
     * considerar handlers específicos por exceção.</p>
     */
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
