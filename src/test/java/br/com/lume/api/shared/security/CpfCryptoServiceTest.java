package br.com.lume.api.shared.security;

import br.com.lume.api.shared.config.CryptoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CpfCryptoService - Testes Unitários")
class CpfCryptoServiceTest {

    private static final String HMAC_KEY = "chave-secreta-hmac-cpf-lume-api-minimo-256-bits";
    private static final String AES_KEY = "chave-secreta-aes-cpf-lume-32byt";
    private static final String CPF_EXEMPLO = "52282865863";

    private CpfCryptoService service;

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties(HMAC_KEY, AES_KEY);
        service = new CpfCryptoService(properties);
    }

    @Nested
    @DisplayName("gerarHash(String)")
    class GerarHash {

        @Test
        @DisplayName("Deve gerar hash determinístico de 64 caracteres hexadecimais")
        void deveGerarHashDeterministico() {
            String hash1 = service.gerarHash(CPF_EXEMPLO);
            String hash2 = service.gerarHash(CPF_EXEMPLO);

            assertThat(hash1)
                    .isNotNull()
                    .hasSize(64)
                    .matches("^[0-9a-f]{64}$")
                    .isEqualTo(hash2);
        }

        @Test
        @DisplayName("Deve gerar hashes diferentes para CPFs diferentes")
        void deveGerarHashesDiferentesParaCpfsDiferentes() {
            String hash1 = service.gerarHash("52282865863");
            String hash2 = service.gerarHash("12345678909");

            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("Deve retornar null quando CPF for nulo")
        void deveRetornarNullQuandoCpfForNulo() {
            assertThat(service.gerarHash(null)).isNull();
        }
    }

    @Nested
    @DisplayName("cifrar(String) e decifrar(String)")
    class CifracaoEDecifracao {

        @Test
        @DisplayName("Deve cifrar CPF gerando payload diferente do texto plano")
        void deveCifrarCpfComSucesso() {
            String cifrado = service.cifrar(CPF_EXEMPLO);

            assertThat(cifrado)
                    .isNotNull()
                    .isNotBlank()
                    .isNotEqualTo(CPF_EXEMPLO);
        }

        @Test
        @DisplayName("Deve gerar cifras diferentes para o mesmo CPF devido ao IV aleatório (probabilístico)")
        void deveGerarCifrasDiferentesComIvAleatorio() {
            String cifrado1 = service.cifrar(CPF_EXEMPLO);
            String cifrado2 = service.cifrar(CPF_EXEMPLO);

            assertThat(cifrado1).isNotEqualTo(cifrado2);
        }

        @Test
        @DisplayName("Deve decifrar com sucesso restaurando o CPF original")
        void deveDecifrarComSucesso() {
            String cifrado = service.cifrar(CPF_EXEMPLO);
            String decifrado = service.decifrar(cifrado);

            assertThat(decifrado).isEqualTo(CPF_EXEMPLO);
        }

        @Test
        @DisplayName("Deve retornar null quando entrada de cifração for nula")
        void deveRetornarNullNaCifracaoComEntradaNula() {
            assertThat(service.cifrar(null)).isNull();
        }

        @Test
        @DisplayName("Deve retornar null quando entrada de decifração for nula ou em branco")
        void deveRetornarNullNaDecifracaoComEntradaNulaOuEmBranco() {
            assertThat(service.decifrar(null)).isNull();
            assertThat(service.decifrar("   ")).isNull();
        }

        @Test
        @DisplayName("Deve lançar exceção quando payload cifrado for corrompido ou menor que o IV")
        void deveLancarExcecaoQuandoPayloadInvalido() {
            assertThatThrownBy(() -> service.decifrar("payload-invalido"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Validação estrita do tamanho da chave AES (32 bytes)")
    class ValidacaoChaveAes {

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando chave AES tiver tamanho diferente de 32 bytes")
        void deveRejeitarChaveAesComTamanhoDiferenteDe32Bytes() {
            CryptoProperties maiorQue32 = new CryptoProperties(HMAC_KEY, "chave-longa-com-mais-de-32-caracteres-para-teste");
            CryptoProperties menorQue32 = new CryptoProperties(HMAC_KEY, "chave-curta");

            assertThat(maiorQue32.isAesKeyValida()).isFalse();
            assertThat(menorQue32.isAesKeyValida()).isFalse();

            assertThatThrownBy(() -> new CpfCryptoService(maiorQue32))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A chave AES do CPF deve conter exatamente 32 bytes (256 bits).");

            assertThatThrownBy(() -> new CpfCryptoService(menorQue32))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A chave AES do CPF deve conter exatamente 32 bytes (256 bits).");
        }
    }
}
