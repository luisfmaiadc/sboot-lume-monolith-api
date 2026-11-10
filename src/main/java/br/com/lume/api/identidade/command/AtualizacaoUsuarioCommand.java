package br.com.lume.api.identidade.command;

import br.com.lume.api.identidade.domain.Genero;
import br.com.lume.api.identidade.domain.OrientacaoSexual;
import br.com.lume.api.identidade.domain.RacaEtnia;

public record AtualizacaoUsuarioCommand(
        String nomeCompleto,
        String telefone,
        Genero genero,
        RacaEtnia racaEtnia,
        OrientacaoSexual orientacaoSexual
) {
}
