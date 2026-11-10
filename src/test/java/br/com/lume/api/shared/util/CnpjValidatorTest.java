package br.com.lume.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CnpjValidator - Testes Unitários")
class CnpjValidatorTest {

    @Nested
    @DisplayName("isValido(String)")
    class IsValido {

        @ParameterizedTest
        @ValueSource(strings = {
                "11222333000181",
                "11222333000009",
                "11444777000161",
                "12ABC34501DE35",
                "12abc34501de35"
        })
        @DisplayName("Deve retornar true quando CNPJ for válido (numérico ou alfanumérico)")
        void deveRetornarTrueQuandoCnpjValido(String cnpj) {
            boolean resultado = CnpjValidator.isValido(cnpj);

            assertThat(resultado).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "123",
                "1122233300018",
                "112223330001810",
                "              "
        })
        @DisplayName("Deve retornar false quando CNPJ for nulo, vazio ou tiver tamanho diferente de 14")
        void deveRetornarFalseQuandoTamanhoInvalidoOuNulo(String cnpj) {
            boolean resultado = CnpjValidator.isValido(cnpj);

            assertThat(resultado).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "11.222.333/000",
                "1122233300018a",
                "112223330001a1",
                "112223330001-1",
                "112223330001/1",
                "112223330001:1",
                "12ABC34501DE#5",
                "12ABC34501DE3A"
        })
        @DisplayName("Deve retornar false quando CNPJ contiver caracteres especiais ou letras nas posições de DV")
        void deveRetornarFalseQuandoConterCaracteresEspeciaisOuLetrasNoDV(String cnpj) {
            boolean resultado = CnpjValidator.isValido(cnpj);

            assertThat(resultado).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "00000000000000",
                "11111111111111",
                "22222222222222",
                "33333333333333",
                "44444444444444",
                "55555555555555",
                "66666666666666",
                "77777777777777",
                "88888888888888",
                "99999999999999",
                "AAAAAAAAAAAAAA",
                "ZZZZZZZZZZZZZZ"
        })
        @DisplayName("Deve retornar false quando CNPJ for formado por sequência de caracteres repetidos")
        void deveRetornarFalseQuandoDigitosRepetidos(String cnpj) {
            boolean resultado = CnpjValidator.isValido(cnpj);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando o 1º dígito verificador for inválido (numérico)")
        void deveRetornarFalseQuandoPrimeiroDigitoVerificadorInvalido() {
            String cnpjComPrimeiroDigitoInvalido = "11222333000191";

            boolean resultado = CnpjValidator.isValido(cnpjComPrimeiroDigitoInvalido);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando o 2º dígito verificador for inválido (numérico)")
        void deveRetornarFalseQuandoSegundoDigitoVerificadorInvalido() {
            String cnpjComSegundoDigitoInvalido = "11222333000182";

            boolean resultado = CnpjValidator.isValido(cnpjComSegundoDigitoInvalido);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve validar CNPJ alfanumérico oficial da Receita Federal (12ABC34501DE35)")
        void deveValidarCnpjAlfanumericoOficialReceitaFederal() {
            assertThat(CnpjValidator.isValido("12ABC34501DE35")).isTrue();
            assertThat(CnpjValidator.isValido("12abc34501de35")).isTrue();
            assertThatCode(() -> CnpjValidator.validar("12ABC34501DE35")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve rejeitar CNPJ alfanumérico com 1º ou 2º dígito verificador incorreto")
        void deveRejeitarCnpjAlfanumericoComDvsIncorretos() {
            assertThat(CnpjValidator.isValido("12ABC34501DE45")).isFalse(); // DV1 incorreto (deveria ser 3)
            assertThat(CnpjValidator.isValido("12ABC34501DE36")).isFalse(); // DV2 incorreto (deveria ser 5)
        }
    }

    @Nested
    @DisplayName("validar(String)")
    class Validar {

        @Test
        @DisplayName("Deve executar sem lançar exceção quando CNPJ for válido")
        void deveExecutarSemExcecaoQuandoCnpjValido() {
            assertThatCode(() -> CnpjValidator.validar("11222333000181"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "00000000000000",
                "11222333000182",
                "123456"
        })
        @DisplayName("Deve lançar BusinessRuleException quando CNPJ for inválido")
        void deveLancarBusinessRuleExceptionQuandoCnpjInvalido(String cnpjInvalido) {
            assertThatThrownBy(() -> CnpjValidator.validar(cnpjInvalido))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("CNPJ informado é inválido.");
        }
    }
}
