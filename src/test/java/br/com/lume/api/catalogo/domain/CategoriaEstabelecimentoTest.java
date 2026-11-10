package br.com.lume.api.catalogo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CategoriaEstabelecimento - Testes Unitários de Domínio")
class CategoriaEstabelecimentoTest {

    @Nested
    @DisplayName("criar(...)")
    class Criar {

        @Test
        @DisplayName("Deve criar CategoriaEstabelecimento com e sem ID, normalizando código e nome")
        void deveCriarCategoriaComSucesso() {
            CategoriaEstabelecimento comId = CategoriaEstabelecimento.criar(
                    1, "  BAR_RESTAURANTE  ", "  Bares e Restaurantes  ", true
            );
            CategoriaEstabelecimento semId = CategoriaEstabelecimento.criar(
                    "CASA_NOTURNA", "Casas Noturnas", false
            );

            assertThat(comId.getId()).isEqualTo(1);
            assertThat(comId.getCodigo()).isEqualTo("BAR_RESTAURANTE");
            assertThat(comId.getNome()).isEqualTo("Bares e Restaurantes");
            assertThat(comId.isAtivo()).isTrue();

            assertThat(semId.getId()).isNull();
            assertThat(semId.getCodigo()).isEqualTo("CASA_NOTURNA");
            assertThat(semId.getNome()).isEqualTo("Casas Noturnas");
            assertThat(semId.isAtivo()).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando código for nulo ou em branco")
        void deveRejeitarCodigoNuloOuEmBranco(String codigoInvalido) {
            assertThatThrownBy(() -> CategoriaEstabelecimento.criar(1, codigoInvalido, "Nome", true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Código da categoria de estabelecimento é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando código exceder 50 caracteres")
        void deveRejeitarCodigoMaiorQue50Caracteres() {
            String codigoLongo = "C".repeat(51);

            assertThatThrownBy(() -> CategoriaEstabelecimento.criar(1, codigoLongo, "Nome", true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Código da categoria de estabelecimento deve ter no máximo 50 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando nome for nulo ou em branco")
        void deveRejeitarNomeNuloOuEmBranco(String nomeInvalido) {
            assertThatThrownBy(() -> CategoriaEstabelecimento.criar(1, "CODIGO", nomeInvalido, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome da categoria de estabelecimento é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando nome exceder 100 caracteres")
        void deveRejeitarNomeMaiorQue100Caracteres() {
            String nomeLongo = "N".repeat(101);

            assertThatThrownBy(() -> CategoriaEstabelecimento.criar(1, "CODIGO", nomeLongo, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome da categoria de estabelecimento deve ter no máximo 100 caracteres.");
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve comparar CategoriaEstabelecimento pela igualdade do código")
        void deveCompararPorCodigo() {
            CategoriaEstabelecimento c1 = CategoriaEstabelecimento.criar(1, "BAR_RESTAURANTE", "Nome 1", true);
            CategoriaEstabelecimento c2 = CategoriaEstabelecimento.criar(2, "BAR_RESTAURANTE", "Nome 2", false);
            CategoriaEstabelecimento cDiferente = CategoriaEstabelecimento.criar(1, "SHOPPING", "Nome 1", true);

            assertThat(c1)
                    .isEqualTo(c1)
                    .isEqualTo(c2)
                    .hasSameHashCodeAs(c2)
                    .isNotEqualTo(cDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("BAR_RESTAURANTE");
        }
    }
}
