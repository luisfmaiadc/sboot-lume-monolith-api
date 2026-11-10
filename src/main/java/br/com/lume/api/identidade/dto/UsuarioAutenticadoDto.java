package br.com.lume.api.identidade.dto;

import java.util.List;
import java.util.UUID;

public record UsuarioAutenticadoDto(
        UUID id,
        String nomeCompleto,
        String email,
        List<String> perfis
) {
    public UsuarioAutenticadoDto {
        perfis = perfis != null ? List.copyOf(perfis) : List.of();
    }
}
