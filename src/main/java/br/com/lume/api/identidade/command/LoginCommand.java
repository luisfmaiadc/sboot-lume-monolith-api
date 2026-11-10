package br.com.lume.api.identidade.command;

public record LoginCommand(
        String email,
        String senha
) {
}
