package org.obeci.platform.exceptions;

/**
 * Exceção lançada quando uma operação tenta criar/atualizar uma turma
 * violando a regra de unicidade: não pode existir mais de uma turma com
 * o mesmo nome dentro da mesma escola.
 *
 * Ela é capturada pelo {@code GlobalExceptionHandler} e convertida em HTTP 409 (Conflict).
 */
public class DuplicateTurmaException extends RuntimeException {
    public DuplicateTurmaException(String message) {
        super(message);
    }
}
