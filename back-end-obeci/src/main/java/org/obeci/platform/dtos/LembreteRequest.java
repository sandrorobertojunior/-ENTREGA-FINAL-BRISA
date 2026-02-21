package org.obeci.platform.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload de criação/edição de lembrete.
 *
 * Observação: o texto pode conter quebras de linha (\n). O front-end deve renderizar
 * usando CSS (ex.: white-space: pre-wrap) para preservar a formatação.
 */
public class LembreteRequest {

    @NotBlank(message = "Lembrete não pode ser vazio")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
