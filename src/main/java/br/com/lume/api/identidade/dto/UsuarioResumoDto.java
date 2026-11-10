package br.com.lume.api.identidade.dto;

import java.util.List;
import java.util.UUID;

public record UsuarioResumoDto(
        UUID id,
        String nomeCompleto,
        boolean ativo,
        List<String> perfis
) {
    public UsuarioResumoDto {
        perfis = perfis != null ? List.copyOf(perfis) : List.of();
    }
}
