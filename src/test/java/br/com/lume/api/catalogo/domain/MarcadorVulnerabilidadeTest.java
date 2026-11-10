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

@DisplayName("MarcadorVulnerabilidade - Testes Unitários de Domínio")
class MarcadorVulnerabilidadeTest {

    @Nested
    @DisplayName("criar(...)")
    class Criar {

        @Test
        @DisplayName("Deve criar MarcadorVulnerabilidade com e sem ID, normalizando código, nome e descrição")
        void deveCriarMarcadorComSucesso() {
            MarcadorVulnerabilidade comId = MarcadorVulnerabilidade.criar(
                    1, "  MISOGINIA  ", "  Misoginia  ", "  Ataque motivado por gênero  ", true
            );
            MarcadorVulnerabilidade semIdEDescNula = MarcadorVulnerabilidade.criar(
                    "RACISMO", "Racismo", null, false
            );

            assertThat(comId.getId()).isEqualTo(1);
            assertThat(comId.getCodigo()).isEqualTo("MISOGINIA");
            assertThat(comId.getNome()).isEqualTo("Misoginia");
            assertThat(comId.getDescricao()).isEqualTo("Ataque motivado por gênero");
            assertThat(comId.isAtivo()).isTrue();

            assertThat(semIdEDescNula.getId()).isNull();
            assertThat(semIdEDescNula.getCodigo()).isEqualTo("RACISMO");
            assertThat(semIdEDescNula.getNome()).isEqualTo("Racismo");
            assertThat(semIdEDescNula.getDescricao()).isNull();
            assertThat(semIdEDescNula.isAtivo()).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando código for nulo ou em branco")
        void deveRejeitarCodigoNuloOuEmBranco(String codigoInvalido) {
            assertThatThrownBy(() -> MarcadorVulnerabilidade.criar(1, codigoInvalido, "Nome", "Desc", true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Código do marcador de vulnerabilidade é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando código exceder 50 caracteres")
        void deveRejeitarCodigoMaiorQue50Caracteres() {
            String codigoLongo = "C".repeat(51);

            assertThatThrownBy(() -> MarcadorVulnerabilidade.criar(1, codigoLongo, "Nome", "Desc", true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Código do marcador de vulnerabilidade deve ter no máximo 50 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando nome for nulo ou em branco")
        void deveRejeitarNomeNuloOuEmBranco(String nomeInvalido) {
            assertThatThrownBy(() -> MarcadorVulnerabilidade.criar(1, "CODIGO", nomeInvalido, "Desc", true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do marcador de vulnerabilidade é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando nome exceder 100 caracteres")
        void deveRejeitarNomeMaiorQue100Caracteres() {
            String nomeLongo = "N".repeat(101);

            assertThatThrownBy(() -> MarcadorVulnerabilidade.criar(1, "CODIGO", nomeLongo, "Desc", true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do marcador de vulnerabilidade deve ter no máximo 100 caracteres.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando descrição exceder 255 caracteres")
        void deveRejeitarDescricaoMaiorQue255Caracteres() {
            String descricaoLonga = "D".repeat(256);

            assertThatThrownBy(() -> MarcadorVulnerabilidade.criar(1, "CODIGO", "Nome", descricaoLonga, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Descrição do marcador de vulnerabilidade deve ter no máximo 255 caracteres.");
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve comparar MarcadorVulnerabilidade pela igualdade do código")
        void deveCompararPorCodigo() {
            MarcadorVulnerabilidade m1 = MarcadorVulnerabilidade.criar(1, "MISOGINIA", "Nome 1", "D1", true);
            MarcadorVulnerabilidade m2 = MarcadorVulnerabilidade.criar(2, "MISOGINIA", "Nome 2", "D2", false);
            MarcadorVulnerabilidade mDiferente = MarcadorVulnerabilidade.criar(1, "LGBTFOBIA", "Nome 1", "D1", true);

            assertThat(m1)
                    .isEqualTo(m1)
                    .isEqualTo(m2)
                    .hasSameHashCodeAs(m2)
                    .isNotEqualTo(mDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("MISOGINIA");
        }
    }
}
