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

@DisplayName("TipoViolencia - Testes Unitários de Domínio")
class TipoViolenciaTest {

    @Nested
    @DisplayName("criar(...)")
    class Criar {

        @Test
        @DisplayName("Deve criar TipoViolencia com e sem ID, normalizando código e nome com trim")
        void deveCriarTipoViolenciaComSucesso() {
            TipoViolencia comId = TipoViolencia.criar(
                    1, "  ASSEDIO_VERBAL  ", "  Assédio Verbal / Importunação  ", 2, 1, true
            );
            TipoViolencia semId = TipoViolencia.criar(
                    "AGRESSAO_FISICA", "Agressão Física", 8, null, false
            );

            assertThat(comId.getId()).isEqualTo(1);
            assertThat(comId.getCodigo()).isEqualTo("ASSEDIO_VERBAL");
            assertThat(comId.getNome()).isEqualTo("Assédio Verbal / Importunação");
            assertThat(comId.getPesoGravidade()).isEqualTo(2);
            assertThat(comId.getOrdemExibicao()).isEqualTo(1);
            assertThat(comId.isAtivo()).isTrue();

            assertThat(semId.getId()).isNull();
            assertThat(semId.getCodigo()).isEqualTo("AGRESSAO_FISICA");
            assertThat(semId.getNome()).isEqualTo("Agressão Física");
            assertThat(semId.getPesoGravidade()).isEqualTo(8);
            assertThat(semId.getOrdemExibicao()).isNull();
            assertThat(semId.isAtivo()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 10})
        @DisplayName("Deve aceitar limites extremos válidos de pesoGravidade (1 e 10)")
        void deveAceitarLimitesExtremosDePesoGravidade(int pesoValido) {
            TipoViolencia tipo = TipoViolencia.criar(1, "CODIGO", "Nome", pesoValido, 1, true);

            assertThat(tipo.getPesoGravidade()).isEqualTo(pesoValido);
        }

        @ParameterizedTest
        @ValueSource(ints = {-10, 0, 11, 99})
        @DisplayName("Deve lançar BusinessRuleException quando pesoGravidade for menor que 1 ou maior que 10")
        void deveRejeitarPesoGravidadeForaDaFaixa(int pesoInvalido) {
            assertThatThrownBy(() -> TipoViolencia.criar(1, "CODIGO", "Nome", pesoInvalido, 1, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Peso de gravidade deve estar entre 1 e 10.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando código for nulo ou em branco")
        void deveRejeitarCodigoNuloOuEmBranco(String codigoInvalido) {
            assertThatThrownBy(() -> TipoViolencia.criar(1, codigoInvalido, "Nome", 5, 1, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Código do tipo de violência é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando código exceder 50 caracteres")
        void deveRejeitarCodigoMaiorQue50Caracteres() {
            String codigoLongo = "C".repeat(51);

            assertThatThrownBy(() -> TipoViolencia.criar(1, codigoLongo, "Nome", 5, 1, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Código do tipo de violência deve ter no máximo 50 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando nome for nulo ou em branco")
        void deveRejeitarNomeNuloOuEmBranco(String nomeInvalido) {
            assertThatThrownBy(() -> TipoViolencia.criar(1, "CODIGO", nomeInvalido, 5, 1, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do tipo de violência é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando nome exceder 100 caracteres")
        void deveRejeitarNomeMaiorQue100Caracteres() {
            String nomeLongo = "N".repeat(101);

            assertThatThrownBy(() -> TipoViolencia.criar(1, "CODIGO", nomeLongo, 5, 1, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do tipo de violência deve ter no máximo 100 caracteres.");
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve comparar TipoViolencia pela igualdade do código")
        void deveCompararPorCodigo() {
            TipoViolencia t1 = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Nome 1", 2, 1, true);
            TipoViolencia t2 = TipoViolencia.criar(2, "ASSEDIO_VERBAL", "Nome 2", 5, 2, false);
            TipoViolencia tDiferente = TipoViolencia.criar(1, "OUTRO_CODIGO", "Nome 1", 2, 1, true);

            assertThat(t1)
                    .isEqualTo(t1)
                    .isEqualTo(t2)
                    .hasSameHashCodeAs(t2)
                    .isNotEqualTo(tDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("ASSEDIO_VERBAL");
        }
    }
}
