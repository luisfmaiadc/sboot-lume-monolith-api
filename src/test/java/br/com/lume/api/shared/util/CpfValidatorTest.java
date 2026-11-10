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

@DisplayName("CpfValidator - Testes Unitários")
class CpfValidatorTest {

    @Nested
    @DisplayName("isValido(String)")
    class IsValido {

        @ParameterizedTest
        @ValueSource(strings = {
                "52998224725",
                "11144477735",
                "12345678909"
        })
        @DisplayName("Deve retornar true quando CPF for válido")
        void deveRetornarTrueQuandoCpfValido(String cpf) {
            boolean resultado = CpfValidator.isValido(cpf);

            assertThat(resultado).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "123",
                "5299822472",
                "529982247250",
                "           "
        })
        @DisplayName("Deve retornar false quando CPF for nulo, vazio ou tiver tamanho diferente de 11")
        void deveRetornarFalseQuandoTamanhoInvalidoOuNulo(String cpf) {
            boolean resultado = CpfValidator.isValido(cpf);

            assertThat(resultado).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "529.982.247",
                "5299822472a",
                "529982247-5",
                "529982247/5",
                "529982247:5"
        })
        @DisplayName("Deve retornar false quando CPF contiver caracteres não-numéricos")
        void deveRetornarFalseQuandoConterCaracteresNaoNumericos(String cpf) {
            boolean resultado = CpfValidator.isValido(cpf);

            assertThat(resultado).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "00000000000",
                "11111111111",
                "22222222222",
                "33333333333",
                "44444444444",
                "55555555555",
                "66666666666",
                "77777777777",
                "88888888888",
                "99999999999"
        })
        @DisplayName("Deve retornar false quando CPF for formado por sequência de dígitos repetidos")
        void deveRetornarFalseQuandoDigitosRepetidos(String cpf) {
            boolean resultado = CpfValidator.isValido(cpf);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando o 1º dígito verificador for inválido")
        void deveRetornarFalseQuandoPrimeiroDigitoVerificadorInvalido() {
            String cpfComPrimeiroDigitoInvalido = "52998224735";

            boolean resultado = CpfValidator.isValido(cpfComPrimeiroDigitoInvalido);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando o 2º dígito verificador for inválido")
        void deveRetornarFalseQuandoSegundoDigitoVerificadorInvalido() {
            String cpfComSegundoDigitoInvalido = "52998224726";

            boolean resultado = CpfValidator.isValido(cpfComSegundoDigitoInvalido);

            assertThat(resultado).isFalse();
        }
    }

    @Nested
    @DisplayName("validar(String)")
    class Validar {

        @Test
        @DisplayName("Deve executar sem lançar exceção quando CPF for válido")
        void deveExecutarSemExcecaoQuandoCpfValido() {
            assertThatCode(() -> CpfValidator.validar("52998224725"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "00000000000",
                "52998224726",
                "123456"
        })
        @DisplayName("Deve lançar BusinessRuleException quando CPF for inválido")
        void deveLancarBusinessRuleExceptionQuandoCpfInvalido(String cpfInvalido) {
            assertThatThrownBy(() -> CpfValidator.validar(cpfInvalido))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("CPF informado é inválido.");
        }
    }
}
