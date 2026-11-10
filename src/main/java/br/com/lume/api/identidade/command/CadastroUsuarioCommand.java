package br.com.lume.api.identidade.command;

import br.com.lume.api.identidade.domain.Genero;
import br.com.lume.api.identidade.domain.OrientacaoSexual;
import br.com.lume.api.identidade.domain.RacaEtnia;

public record CadastroUsuarioCommand(
        String nomeCompleto,
        String cpf,
        String telefone,
        String email,
        String senha,
        Genero genero,
        RacaEtnia racaEtnia,
        OrientacaoSexual orientacaoSexual
) {
}
