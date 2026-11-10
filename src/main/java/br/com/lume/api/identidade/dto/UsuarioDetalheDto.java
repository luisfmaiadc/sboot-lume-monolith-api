package br.com.lume.api.identidade.dto;

import br.com.lume.api.identidade.domain.Genero;
import br.com.lume.api.identidade.domain.OrientacaoSexual;
import br.com.lume.api.identidade.domain.RacaEtnia;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UsuarioDetalheDto(
        UUID id,
        String nomeCompleto,
        String cpf,
        String telefone,
        String email,
        Genero genero,
        RacaEtnia racaEtnia,
        OrientacaoSexual orientacaoSexual,
        List<String> perfis,
        boolean ativo,
        OffsetDateTime criadoEm
) {
    public UsuarioDetalheDto {
        perfis = perfis != null ? List.copyOf(perfis) : List.of();
    }
}
