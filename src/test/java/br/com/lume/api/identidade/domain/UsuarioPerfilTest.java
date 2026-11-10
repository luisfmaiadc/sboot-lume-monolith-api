package br.com.lume.api.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UsuarioPerfil e UsuarioPerfilId - Testes Unitários de Domínio")
class UsuarioPerfilTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final String CPF_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private Usuario usuario;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        usuario = Usuario.criar(
                "Maria da Silva",
                "52998224725",
                CPF_HASH,
                "11987654321",
                null,
                null,
                null,
                AGORA
        );
        perfil = Perfil.criar(1, "ROLE_USUARIO", "Perfil cidadão");
    }

    @Nested
    @DisplayName("vincular(Usuario, Perfil, OffsetDateTime, UUID)")
    class Vincular {

        @Test
        @DisplayName("Deve vincular usuário e perfil preenchendo chave composta e metadados")
        void deveVincularUsuarioEPerfilComSucesso() {
            UUID atribuidoPor = UUID.randomUUID();

            UsuarioPerfil vinculo = UsuarioPerfil.vincular(usuario, perfil, AGORA, atribuidoPor);

            assertThat(vinculo.getId()).isEqualTo(new UsuarioPerfilId(usuario.getId(), perfil.getId()));
            assertThat(vinculo.getId().getUsuarioId()).isEqualTo(usuario.getId());
            assertThat(vinculo.getId().getPerfilId()).isEqualTo(1);
            assertThat(vinculo.getUsuario()).isEqualTo(usuario);
            assertThat(vinculo.getPerfil()).isEqualTo(perfil);
            assertThat(vinculo.getAtribuidoEm()).isEqualTo(AGORA);
            assertThat(vinculo.getAtribuidoPor()).isEqualTo(atribuidoPor);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando usuário, perfil ou agora forem nulos")
        void deveRejeitarParametrosObrigatoriosNulos() {
            assertThatThrownBy(() -> UsuarioPerfil.vincular(null, perfil, AGORA, usuario.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Usuário é obrigatório para vínculo de perfil.");

            assertThatThrownBy(() -> UsuarioPerfil.vincular(usuario, null, AGORA, usuario.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Perfil é obrigatório para vínculo ao usuário.");

            assertThatThrownBy(() -> UsuarioPerfil.vincular(usuario, perfil, null, usuario.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para vínculo de perfil.");
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve comparar UsuarioPerfil por usuarioId e nome do perfil")
        void deveCompararUsuarioPerfilCorretamente() {
            UsuarioPerfil vinculo1 = UsuarioPerfil.vincular(usuario, perfil, AGORA, usuario.getId());
            UsuarioPerfil vinculo2 = UsuarioPerfil.vincular(usuario, perfil, AGORA.plusMinutes(5), UUID.randomUUID());
            Perfil outroPerfil = Perfil.criar(2, "ROLE_GESTOR", "Gestor");
            UsuarioPerfil vinculoDiferente = UsuarioPerfil.vincular(usuario, outroPerfil, AGORA, usuario.getId());

            assertThat(vinculo1)
                    .isEqualTo(vinculo1)
                    .isEqualTo(vinculo2)
                    .hasSameHashCodeAs(vinculo2)
                    .isNotEqualTo(vinculoDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("outroObjeto");

            UsuarioPerfilId id1 = new UsuarioPerfilId(usuario.getId(), 1);
            UsuarioPerfilId id2 = new UsuarioPerfilId(usuario.getId(), 1);
            UsuarioPerfilId idDiferente = new UsuarioPerfilId(usuario.getId(), 2);

            assertThat(id1)
                    .isEqualTo(id2)
                    .hasSameHashCodeAs(id2)
                    .isNotEqualTo(idDiferente);
        }
    }
}
