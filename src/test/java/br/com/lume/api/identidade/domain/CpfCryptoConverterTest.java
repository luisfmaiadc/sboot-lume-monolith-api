package br.com.lume.api.identidade.domain;

import br.com.lume.api.shared.config.CryptoProperties;
import br.com.lume.api.shared.security.CpfCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CpfCryptoConverter - Testes Unitários")
class CpfCryptoConverterTest {

    private static final String HMAC_KEY = "chave-secreta-hmac-cpf-lume-api-minimo-256-bits";
    private static final String AES_KEY = "chave-secreta-aes-cpf-lume-32byt";
    private static final String CPF_EXEMPLO = "52282865863";

    private CpfCryptoService service;
    private CpfCryptoConverter converter;

    @BeforeEach
    void setUp() {
        service = new CpfCryptoService(new CryptoProperties(HMAC_KEY, AES_KEY));
        converter = new CpfCryptoConverter(service);
    }

    @Nested
    @DisplayName("convertToDatabaseColumn(String)")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("Deve cifrar o CPF antes de persistir no banco de dados")
        void deveCifrarCpfParaPersistencia() {
            String dbColumn = converter.convertToDatabaseColumn(CPF_EXEMPLO);

            assertThat(dbColumn)
                    .isNotNull()
                    .isNotBlank()
                    .isNotEqualTo(CPF_EXEMPLO);
        }

        @Test
        @DisplayName("Deve retornar null quando atributo for nulo")
        void deveRetornarNullQuandoAtributoNulo() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute(String)")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("Deve decifrar o valor da coluna restaurando o CPF original na entidade")
        void deveDecifrarColunaParaEntidade() {
            String dbColumn = converter.convertToDatabaseColumn(CPF_EXEMPLO);
            String entidadeAttr = converter.convertToEntityAttribute(dbColumn);

            assertThat(entidadeAttr).isEqualTo(CPF_EXEMPLO);
        }

        @Test
        @DisplayName("Deve retornar null quando dbData for nulo")
        void deveRetornarNullQuandoDbDataNulo() {
            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }
}
