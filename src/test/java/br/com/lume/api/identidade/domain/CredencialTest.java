package br.com.lume.api.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.AccountLockedException;
import br.com.lume.api.shared.exception.BusinessRuleException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Credencial - Testes Unitários de Domínio")
class CredencialTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final String CPF_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final int MAX_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;
    private static final int TENTATIVAS_APOS_DESBLOQUEIO = 3;

    private Usuario usuario;

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
    }

    @Nested
    @DisplayName("criar(Usuario, String, String, OffsetDateTime)")
    class Criar {

        @Test
        @DisplayName("Deve criar credencial com e-mail normalizado em minúsculas e contadores zerados")
        void deveCriarCredencialComEmailNormalizado() {
            Credencial credencial = Credencial.criar(
                    usuario,
                    "  Maria.Silva@Exemplo.COM  ",
                    "$2a$12$hashBcryptValido",
                    AGORA
            );

            assertThat(credencial.getId()).isNotNull();
            assertThat(credencial.getUsuario()).isEqualTo(usuario);
            assertThat(credencial.getEmail()).isEqualTo("maria.silva@exemplo.com");
            assertThat(credencial.getHashSenha()).isEqualTo("$2a$12$hashBcryptValido");
            assertThat(credencial.getTentativasFalhas()).isZero();
            assertThat(credencial.getBloqueadoAte()).isNull();
            assertThat(credencial.getUltimoLoginEm()).isNull();
            assertThat(credencial.isRequerRedefinicaoSenha()).isFalse();
            assertThat(credencial.getCriadoEm()).isEqualTo(AGORA);
            assertThat(credencial.getAtualizadoEm()).isNull();
        }

        @Test
        @DisplayName("Deve rejeitar criação quando usuário for nulo")
        void deveRejeitarUsuarioNulo() {
            assertThatThrownBy(() -> Credencial.criar(null, "maria@exemplo.com", "hash", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Usuário é obrigatório para criação da credencial.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve rejeitar criação quando e-mail for nulo ou em branco")
        void deveRejeitarEmailNuloOuEmBranco(String emailInvalido) {
            assertThatThrownBy(() -> Credencial.criar(usuario, emailInvalido, "hash", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("E-mail é obrigatório.");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "emailsemarroba.com",
                "@dominio.com",
                "usuario@"
        })
        @DisplayName("Deve rejeitar criação quando formato de e-mail for inválido ou exceder 255 caracteres")
        void deveRejeitarEmailFormatoInvalidoOuMuitoLongo(String emailInvalido) {
            assertThatThrownBy(() -> Credencial.criar(usuario, emailInvalido, "hash", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("E-mail informado é inválido.");

            String emailLongoDemais = "a".repeat(250) + "@ex.com";
            assertThatThrownBy(() -> Credencial.criar(usuario, emailLongoDemais, "hash", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("E-mail informado é inválido.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve rejeitar criação quando hashSenha for nulo ou em branco")
        void deveRejeitarHashSenhaNuloOuEmBranco(String hashInvalido) {
            assertThatThrownBy(() -> Credencial.criar(usuario, "maria@exemplo.com", hashInvalido, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Hash de senha é obrigatório.");
        }

        @Test
        @DisplayName("Deve rejeitar criação quando referência temporal (agora) for nula")
        void deveRejeitarAgoraNulo() {
            assertThatThrownBy(() -> Credencial.criar(usuario, "maria@exemplo.com", "hash", null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para criação da credencial.");
        }
    }

    @Nested
    @DisplayName("Máquina de Estado de Lockout (registrarFalhaLogin, verificarBloqueio, registrarSucessoLogin)")
    class MaquinaEstadoLockout {

        @Test
        @DisplayName("Deve incrementar tentativas falhas de 1 a 4 sem bloquear a conta")
        void deveIncrementarTentativas1A4SemBloquear() {
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);

            for (int tentativa = 1; tentativa <= 4; tentativa++) {
                OffsetDateTime instanteTentativa = AGORA.plusMinutes(tentativa);
                credencial.registrarFalhaLogin(
                        instanteTentativa, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
                );

                assertThat(credencial.getTentativasFalhas()).isEqualTo(tentativa);
                assertThat(credencial.getBloqueadoAte()).isNull();
                assertThat(credencial.isBloqueada(instanteTentativa)).isFalse();
                assertThatCode(() -> credencial.verificarBloqueio(instanteTentativa))
                        .doesNotThrowAnyException();
                assertThat(credencial.getAtualizadoEm()).isEqualTo(instanteTentativa);
            }
        }

        @Test
        @DisplayName("Deve bloquear a conta na 5ª tentativa por 15 minutos e lançar AccountLockedException")
        void deveBloquearNaQuintaTentativaPor15Minutos() {
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);

            for (int i = 1; i <= 4; i++) {
                credencial.registrarFalhaLogin(
                        AGORA.plusSeconds(i), MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
                );
            }

            OffsetDateTime momentoQuintaFalha = AGORA.plusMinutes(1);
            credencial.registrarFalhaLogin(
                    momentoQuintaFalha, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
            );

            OffsetDateTime bloqueioEsperado = momentoQuintaFalha.plusMinutes(MINUTOS_BLOQUEIO);
            assertThat(credencial.getTentativasFalhas()).isEqualTo(5);
            assertThat(credencial.getBloqueadoAte()).isEqualTo(bloqueioEsperado);
            assertThat(credencial.isBloqueada(momentoQuintaFalha)).isTrue();
            assertThat(credencial.isBloqueada(bloqueioEsperado.minusSeconds(1))).isTrue();
            assertThatThrownBy(() -> credencial.verificarBloqueio(momentoQuintaFalha))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("Conta temporariamente bloqueada por excesso de tentativas falhas.");
        }

        @Test
        @DisplayName("Após expirar os 15 minutos de bloqueio, deve permitir exatamente 3 falhas antes de bloquear novamente")
        void devePermitirTresTentativasAposExpiracaoDoBloqueioAntesDeBloquearNovamente() {
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);

            for (int i = 1; i <= 5; i++) {
                credencial.registrarFalhaLogin(
                        AGORA, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
                );
            }
            OffsetDateTime fimPrimeiroBloqueio = AGORA.plusMinutes(MINUTOS_BLOQUEIO);

            // Exatamente no instante bloqueadoAte (e 1 segundo depois), não está mais bloqueada
            assertThat(credencial.isBloqueada(fimPrimeiroBloqueio)).isFalse();
            assertThat(credencial.isBloqueada(fimPrimeiroBloqueio.plusSeconds(1))).isFalse();
            assertThatCode(() -> credencial.verificarBloqueio(fimPrimeiroBloqueio)).doesNotThrowAnyException();

            // 1ª falha pós-expiração: reseta bloqueadoAte e define tentativasFalhas = (5 - 3) + 1 = 3 (não bloqueia)
            OffsetDateTime posExpiracao1 = fimPrimeiroBloqueio.plusSeconds(1);
            credencial.registrarFalhaLogin(
                    posExpiracao1, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
            );
            assertThat(credencial.getTentativasFalhas()).isEqualTo(3);
            assertThat(credencial.getBloqueadoAte()).isNull();
            assertThat(credencial.isBloqueada(posExpiracao1)).isFalse();

            // 2ª falha pós-expiração: tentativasFalhas = 4 (não bloqueia)
            OffsetDateTime posExpiracao2 = posExpiracao1.plusSeconds(10);
            credencial.registrarFalhaLogin(
                    posExpiracao2, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
            );
            assertThat(credencial.getTentativasFalhas()).isEqualTo(4);
            assertThat(credencial.getBloqueadoAte()).isNull();
            assertThat(credencial.isBloqueada(posExpiracao2)).isFalse();

            // 3ª falha pós-expiração: tentativasFalhas = 5 -> bloqueia novamente por 15 minutos
            OffsetDateTime posExpiracao3 = posExpiracao2.plusSeconds(10);
            credencial.registrarFalhaLogin(
                    posExpiracao3, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
            );
            assertThat(credencial.getTentativasFalhas()).isEqualTo(5);
            assertThat(credencial.getBloqueadoAte()).isEqualTo(posExpiracao3.plusMinutes(MINUTOS_BLOQUEIO));
            assertThat(credencial.isBloqueada(posExpiracao3)).isTrue();
            assertThatThrownBy(() -> credencial.verificarBloqueio(posExpiracao3))
                    .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @DisplayName("Deve resetar tentativasFalhas e bloqueadoAte e atualizar ultimoLoginEm em registrarSucessoLogin")
        void deveResetarEstadoAoRegistrarSucessoLogin() {
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);
            for (int i = 1; i <= 5; i++) {
                credencial.registrarFalhaLogin(
                        AGORA, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
                );
            }

            OffsetDateTime momentoLoginAposExpiracao = AGORA.plusMinutes(MINUTOS_BLOQUEIO).plusSeconds(5);
            credencial.registrarSucessoLogin(momentoLoginAposExpiracao);

            assertThat(credencial.getTentativasFalhas()).isZero();
            assertThat(credencial.getBloqueadoAte()).isNull();
            assertThat(credencial.getUltimoLoginEm()).isEqualTo(momentoLoginAposExpiracao);
            assertThat(credencial.getAtualizadoEm()).isEqualTo(momentoLoginAposExpiracao);
        }

        @Test
        @DisplayName("Deve lançar AccountLockedException em registrarSucessoLogin quando conta ainda estiver bloqueada")
        void deveLancarAccountLockedExceptionEmSucessoLoginSeAindaBloqueada() {
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);
            for (int i = 1; i <= 5; i++) {
                credencial.registrarFalhaLogin(
                        AGORA, MAX_TENTATIVAS, MINUTOS_BLOQUEIO, TENTATIVAS_APOS_DESBLOQUEIO
                );
            }

            OffsetDateTime duranteBloqueio = AGORA.plusMinutes(5);
            assertThatThrownBy(() -> credencial.registrarSucessoLogin(duranteBloqueio))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("Conta temporariamente bloqueada por excesso de tentativas falhas.");
        }

        @Test
        @DisplayName("Deve validar parâmetros obrigatórios e positivos nos métodos de bloqueio e login")
        void deveValidarParametrosDeTempoEPoliticaDeBloqueio() {
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);

            assertThatThrownBy(() -> credencial.isBloqueada(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para verificação de bloqueio.");

            assertThatThrownBy(() -> credencial.registrarFalhaLogin(null, 5, 15, 3))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para registrar falha de login.");

            assertThatThrownBy(() -> credencial.registrarFalhaLogin(AGORA, 0, 15, 3))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Parâmetros de política de bloqueio devem ser positivos.");

            assertThatThrownBy(() -> credencial.registrarFalhaLogin(AGORA, 5, 0, 3))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Parâmetros de política de bloqueio devem ser positivos.");

            assertThatThrownBy(() -> credencial.registrarFalhaLogin(AGORA, 5, 15, 0))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Parâmetros de política de bloqueio devem ser positivos.");

            assertThatThrownBy(() -> credencial.registrarSucessoLogin(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para registrar sucesso de login.");
        }
    }
}
