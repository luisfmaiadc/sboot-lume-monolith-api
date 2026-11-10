package br.com.lume.api.identidade.dto;

public record TokenResultDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        UsuarioAutenticadoDto usuario
) {}
