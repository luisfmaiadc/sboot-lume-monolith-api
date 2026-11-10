package br.com.lume.api.ocorrencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.shared.config.ReputacaoProperties;
import br.com.lume.api.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CalculadoraReputacaoRiscoService - Testes Unitários")
class CalculadoraReputacaoRiscoServiceTest {

    private static final LocalDate DATA_REFERENCIA = LocalDate.of(2026, 5, 10);

    private CalculadoraReputacaoRiscoService calculadora;

    @BeforeEach
    void setUp() {
        ReputacaoProperties properties = new ReputacaoProperties(
                new BigDecimal("1.00"),
                new BigDecimal("0.70"),
                new BigDecimal("0.40"),
                new BigDecimal("0.10"),
                new BigDecimal("0.15")
        );
        calculadora = new CalculadoraReputacaoRiscoService(properties);
    }

    @Nested
    @DisplayName("calcularFatorDecaimentoTemporal")
    class CalcularFatorDecaimentoTemporal {

        @ParameterizedTest
        @CsvSource({
                "0, 1.00",
                "30, 1.00",
                "31, 0.70",
                "90, 0.70",
                "91, 0.40",
                "365, 0.40",
                "366, 0.10",
                "800, 0.10"
        })
        @DisplayName("Deve retornar fator de decaimento temporal exato conforme faixa de dias decorridos")
        void deveRetornarFatorDecaimentoTemporalConformeFaixaDeDias(long diasAtras, String fatorEsperado) {
            // Arrange
            LocalDate dataOcorrencia = DATA_REFERENCIA.minusDays(diasAtras);

            // Act
            BigDecimal fator = calculadora.calcularFatorDecaimentoTemporal(dataOcorrencia, DATA_REFERENCIA);

            // Assert
            assertThat(fator).isEqualByComparingTo(new BigDecimal(fatorEsperado));
        }

        @Test
        @DisplayName("Deve retornar fatorDias30 quando dataOcorrencia for posterior à dataReferencia")
        void deveRetornarFatorDias30QuandoDataOcorrenciaPosteriorAReferencia() {
            // Arrange
            LocalDate dataFutura = DATA_REFERENCIA.plusDays(2);

            // Act
            BigDecimal fator = calculadora.calcularFatorDecaimentoTemporal(dataFutura, DATA_REFERENCIA);

            // Assert
            assertThat(fator).isEqualByComparingTo(new BigDecimal("1.00"));
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando datas forem nulas")
        void deveLancarBusinessRuleExceptionQuandoDatasNulas() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> calculadora.calcularFatorDecaimentoTemporal(null, DATA_REFERENCIA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Datas de ocorrência e de referência são obrigatórias para o cálculo de decaimento temporal.");

            assertThatThrownBy(() -> calculadora.calcularFatorDecaimentoTemporal(DATA_REFERENCIA, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Datas de ocorrência e de referência são obrigatórias para o cálculo de decaimento temporal.");
        }
    }

    @Nested
    @DisplayName("calcularImpactoOcorrencia")
    class CalcularImpactoOcorrencia {

        @Test
        @DisplayName("Deve calcular impacto da ocorrência aplicando pesoGravidade, fatorTempo e multiplicador de validações")
        void deveCalcularImpactoOcorrenciaCorretamente() {
            // Arrange: peso = 5, 10 dias atrás (fator 1.00), 2 validações (1 + 2 * 0.15 = 1.30) -> 5 * 1.00 * 1.30 = 6.50
            LocalDate dataOcorrencia = DATA_REFERENCIA.minusDays(10);

            // Act
            BigDecimal impacto = calculadora.calcularImpactoOcorrencia(5, dataOcorrencia, 2, DATA_REFERENCIA);

            // Assert
            assertThat(impacto).isEqualByComparingTo(new BigDecimal("6.50"));
        }

        @Test
        @DisplayName("Deve limitar o número de validações computadas a MAXIMO_VALIDACOES_COMPUTADAS (10)")
        void deveLimitarValidacoesComputadasNoTetoMaximo() {
            // Arrange: peso = 5, 10 dias atrás (fator 1.00), 10 validações -> 5 * 1.00 * (1 + 10 * 0.15) = 12.50
            LocalDate dataOcorrencia = DATA_REFERENCIA.minusDays(10);

            // Act
            BigDecimal impactoNoTeto = calculadora.calcularImpactoOcorrencia(5, dataOcorrencia, 10, DATA_REFERENCIA);
            BigDecimal impactoAcimaDoTeto15 = calculadora.calcularImpactoOcorrencia(5, dataOcorrencia, 15, DATA_REFERENCIA);
            BigDecimal impactoAcimaDoTeto100 = calculadora.calcularImpactoOcorrencia(5, dataOcorrencia, 100, DATA_REFERENCIA);

            // Assert
            assertThat(impactoNoTeto).isEqualByComparingTo(new BigDecimal("12.50"));
            assertThat(impactoAcimaDoTeto15).isEqualByComparingTo(impactoNoTeto);
            assertThat(impactoAcimaDoTeto100).isEqualByComparingTo(impactoNoTeto);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando pesoGravidade não for positivo ou totalValidacoes for negativo")
        void deveLancarBusinessRuleExceptionQuandoParametrosInvalidos() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> calculadora.calcularImpactoOcorrencia(0, DATA_REFERENCIA, 0, DATA_REFERENCIA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Peso de gravidade deve ser positivo.");

            assertThatThrownBy(() -> calculadora.calcularImpactoOcorrencia(3, DATA_REFERENCIA, -1, DATA_REFERENCIA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Total de validações não pode ser negativo.");
        }
    }

    @Nested
    @DisplayName("calcularScoreEstabelecimento")
    class CalcularScoreEstabelecimento {

        @Test
        @DisplayName("Deve retornar score máximo 5.00 quando somaImpactos for nula, zero ou negativa")
        void deveRetornarScoreMaximoQuandoSomaImpactosNulaOuZero() {
            // Arrange, Act & Assert
            assertThat(calculadora.calcularScoreEstabelecimento(null)).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(calculadora.calcularScoreEstabelecimento(BigDecimal.ZERO)).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(calculadora.calcularScoreEstabelecimento(new BigDecimal("-1.00"))).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Deve deduzir 0.10 por ponto de impacto e respeitar piso mínimo de 0.00")
        void deveDeduzirImpactoERespeitarPisoZero() {
            // Arrange & Act
            BigDecimal scoreParcial = calculadora.calcularScoreEstabelecimento(new BigDecimal("12.50")); // 5.00 - 1.25 = 3.75
            BigDecimal scoreNoPiso = calculadora.calcularScoreEstabelecimento(new BigDecimal("80.00")); // 5.00 - 8.00 -> 0.00

            // Assert
            assertThat(scoreParcial).isEqualByComparingTo(new BigDecimal("3.75"));
            assertThat(scoreNoPiso).isEqualByComparingTo(new BigDecimal("0.00"));
        }
    }

    @Nested
    @DisplayName("calcularScoreRiscoViaPublica e classificarNivelRisco")
    class ScoreRiscoENivelRisco {

        @Test
        @DisplayName("Deve retornar score de risco 0.00 quando somaImpactos for nula ou zero e limitar teto em 10.00")
        void deveCalcularScoreRiscoViaPublicaRespeitandoLimites() {
            // Arrange & Act
            BigDecimal scoreZero = calculadora.calcularScoreRiscoViaPublica(null);
            BigDecimal scoreIntermediario = calculadora.calcularScoreRiscoViaPublica(new BigDecimal("15.00")); // 15 * 0.20 = 3.00
            BigDecimal scoreTeto = calculadora.calcularScoreRiscoViaPublica(new BigDecimal("100.00")); // 100 * 0.20 -> 10.00

            // Assert
            assertThat(scoreZero).isEqualByComparingTo(new BigDecimal("0.00"));
            assertThat(scoreIntermediario).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(scoreTeto).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @ParameterizedTest
        @CsvSource({
                "0.00, BAIXO",
                "2.49, BAIXO",
                "2.50, MEDIO",
                "4.99, MEDIO",
                "5.00, ALTO",
                "7.49, ALTO",
                "7.50, CRITICO",
                "10.00, CRITICO"
        })
        @DisplayName("Deve classificar corretamente o NivelRisco para todas as fronteiras de score")
        void deveClassificarNivelRiscoNasFronteiras(String scoreStr, NivelRisco nivelEsperado) {
            // Arrange
            BigDecimal score = new BigDecimal(scoreStr);

            // Act
            NivelRisco resultado = calculadora.classificarNivelRisco(score);

            // Assert
            assertThat(resultado).isEqualTo(nivelEsperado);
        }

        @Test
        @DisplayName("Deve classificar como BAIXO quando scoreRisco for nulo")
        void deveClassificarComoBaixoQuandoScoreNulo() {
            // Arrange, Act & Assert
            assertThat(calculadora.classificarNivelRisco(null)).isEqualTo(NivelRisco.BAIXO);
        }
    }
}
