package br.com.lume.api.identidade.command;

public record RefreshTokenCommand(
        String refreshToken
) {
}
