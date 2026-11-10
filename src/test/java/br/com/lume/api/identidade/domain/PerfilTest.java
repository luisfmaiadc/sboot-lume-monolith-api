package br.com.lume.api.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Perfil - Testes Unitários de Domínio")
class PerfilTest {

    @Nested
    @DisplayName("criar(...)")
    class Criar {

        @Test
        @DisplayName("Deve criar perfil com ID, nome e descrição normalizados com trim")
        void deveCriarPerfilComSucesso() {
            Perfil perfilComId = Perfil.criar(1, "  ROLE_USUARIO  ", "  Perfil padrão de usuário  ");
            Perfil perfilSemId = Perfil.criar("ROLE_GESTOR", null);

            assertThat(perfilComId.getId()).isEqualTo(1);
            assertThat(perfilComId.getNome()).isEqualTo("ROLE_USUARIO");
            assertThat(perfilComId.getDescricao()).isEqualTo("Perfil padrão de usuário");

            assertThat(perfilSemId.getId()).isNull();
            assertThat(perfilSemId.getNome()).isEqualTo("ROLE_GESTOR");
            assertThat(perfilSemId.getDescricao()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando nome do perfil for nulo ou em branco")
        void deveRejeitarNomeNuloOuEmBranco(String nomeInvalido) {
            assertThatThrownBy(() -> Perfil.criar(nomeInvalido, "Descrição"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do perfil é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando nome do perfil exceder 50 caracteres")
        void deveRejeitarNomeMaiorQue50Caracteres() {
            String nomeLongo = "R".repeat(51);

            assertThatThrownBy(() -> Perfil.criar(nomeLongo, "Descrição"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do perfil deve ter no máximo 50 caracteres.");
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve considerar iguais perfis com o mesmo nome independentemente do ID")
        void deveCompararPerfisPorNome() {
            Perfil perfil1 = Perfil.criar(1, "ROLE_USUARIO", "Desc 1");
            Perfil perfil2 = Perfil.criar(2, "ROLE_USUARIO", "Desc 2");
            Perfil perfilDiferente = Perfil.criar(1, "ROLE_GESTOR", "Desc 1");

            assertThat(perfil1)
                    .isEqualTo(perfil1)
                    .isEqualTo(perfil2)
                    .hasSameHashCodeAs(perfil2)
                    .isNotEqualTo(perfilDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("ROLE_USUARIO");
        }
    }
}
