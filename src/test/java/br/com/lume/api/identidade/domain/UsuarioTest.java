package br.com.lume.api.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.UnauthorizedException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Usuario - Testes Unitários de Domínio")
class UsuarioTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final String CPF_VALIDO = "52998224725";
    private static final String CPF_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String TELEFONE_VALIDO = "11987654321";
    private static final String NOME_VALIDO = "Maria da Silva";

    @Nested
    @DisplayName("criar(...)")
    class Criar {

        @Test
        @DisplayName("Deve criar usuário ativo com dados válidos e aplicar trim em nome e telefone")
        void deveCriarUsuarioComDadosValidos() {
            Usuario usuario = Usuario.criar(
                    "  Maria da Silva  ",
                    CPF_VALIDO,
                    CPF_HASH,
                    "  11987654321  ",
                    Genero.MULHER_CIS,
                    RacaEtnia.PARDA,
                    OrientacaoSexual.BISSEXUAL,
                    AGORA
            );

            assertThat(usuario.getId()).isNotNull();
            assertThat(usuario.getNomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(usuario.getCpf()).isEqualTo(CPF_VALIDO);
            assertThat(usuario.getCpfHash()).isEqualTo(CPF_HASH);
            assertThat(usuario.getTelefone()).isEqualTo("11987654321");
            assertThat(usuario.isAtivo()).isTrue();
            assertThat(usuario.getGenero()).isEqualTo(Genero.MULHER_CIS);
            assertThat(usuario.getRacaEtnia()).isEqualTo(RacaEtnia.PARDA);
            assertThat(usuario.getOrientacaoSexual()).isEqualTo(OrientacaoSexual.BISSEXUAL);
            assertThat(usuario.getCriadoEm()).isEqualTo(AGORA);
            assertThat(usuario.getCriadoPor()).isEqualTo(usuario.getId());
            assertThat(usuario.getAtualizadoEm()).isNull();
            assertThat(usuario.getPerfis()).isEmpty();
        }

        @Test
        @DisplayName("Deve normalizar gênero, raça/etnia e orientação sexual para PREFIRO_NAO_INFORMAR quando nulos")
        void deveNormalizarDadosDemograficosQuandoNulos() {
            Usuario usuario = Usuario.criar(
                    NOME_VALIDO,
                    CPF_VALIDO,
                    CPF_HASH,
                    TELEFONE_VALIDO,
                    null,
                    null,
                    null,
                    AGORA
            );

            assertThat(usuario.getGenero()).isEqualTo(Genero.PREFIRO_NAO_INFORMAR);
            assertThat(usuario.getRacaEtnia()).isEqualTo(RacaEtnia.PREFIRO_NAO_INFORMAR);
            assertThat(usuario.getOrientacaoSexual()).isEqualTo(OrientacaoSexual.PREFIRO_NAO_INFORMAR);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando nomeCompleto for nulo ou em branco")
        void deveRejeitarNomeCompletoNuloOuEmBranco(String nomeInvalido) {
            assertThatThrownBy(() -> Usuario.criar(
                    nomeInvalido, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome completo é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando nomeCompleto tiver menos de 3 ou mais de 150 caracteres")
        void deveRejeitarNomeCompletoForaDosLimitesDeTamanho() {
            String nomeCurto = "Ab";
            String nomeLongo = "A".repeat(151);

            assertThatThrownBy(() -> Usuario.criar(
                    nomeCurto, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome completo deve ter entre 3 e 150 caracteres.");

            assertThatThrownBy(() -> Usuario.criar(
                    nomeLongo, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome completo deve ter entre 3 e 150 caracteres.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando CPF for inválido")
        void deveRejeitarCpfInvalido() {
            assertThatThrownBy(() -> Usuario.criar(
                    NOME_VALIDO, "11111111111", CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("CPF informado é inválido.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando hash do CPF for nulo ou em branco")
        void deveRejeitarCpfHashNuloOuEmBranco(String hashInvalido) {
            assertThatThrownBy(() -> Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, hashInvalido, TELEFONE_VALIDO, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Hash do CPF é obrigatório.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando telefone for nulo ou em branco")
        void deveRejeitarTelefoneNuloOuEmBranco(String telefoneInvalido) {
            assertThatThrownBy(() -> Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, CPF_HASH, telefoneInvalido, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Telefone é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando telefone tiver menos de 10 ou mais de 11 dígitos")
        void deveRejeitarTelefoneForaDosLimitesDeTamanho() {
            String telefoneCurto = "119876543";
            String telefoneLongo = "1".repeat(12);

            assertThatThrownBy(() -> Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, CPF_HASH, telefoneCurto, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Telefone deve conter entre 10 e 11 dígitos numéricos.");

            assertThatThrownBy(() -> Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, CPF_HASH, telefoneLongo, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Telefone deve conter entre 10 e 11 dígitos numéricos.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando referência temporal (agora) for nula")
        void deveRejeitarAgoraNulo() {
            assertThatThrownBy(() -> Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, null
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para criação do usuário.");
        }
    }

    @Nested
    @DisplayName("adicionarPerfil, getNomesPerfis e possuiPerfil")
    class GerenciamentoPerfis {

        @Test
        @DisplayName("Deve adicionar perfis sem duplicar, listar nomes ordenados e verificar possuiPerfil")
        void deveAdicionarPerfisEVerificarExistencia() {
            Usuario usuario = Usuario.criar(NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA);
            Perfil perfilUsuario = Perfil.criar(1, "ROLE_USUARIO", "Perfil cidadão");
            Perfil perfilGestor = Perfil.criar(2, "ROLE_GESTOR", "Perfil gestor");

            usuario.adicionarPerfil(perfilUsuario, AGORA, usuario.getId());
            usuario.adicionarPerfil(perfilUsuario, AGORA.plusMinutes(1), usuario.getId());
            usuario.adicionarPerfil(perfilGestor, AGORA.plusMinutes(2), usuario.getId());

            assertThat(usuario.getPerfis()).hasSize(2);
            assertThat(usuario.getNomesPerfis()).containsExactly("ROLE_GESTOR", "ROLE_USUARIO");
            assertThat(usuario.possuiPerfil("ROLE_USUARIO")).isTrue();
            assertThat(usuario.possuiPerfil("ROLE_GESTOR")).isTrue();
            assertThat(usuario.possuiPerfil("ROLE_ADMIN")).isFalse();
            assertThat(usuario.possuiPerfil(null)).isFalse();
            assertThat(usuario.possuiPerfil("   ")).isFalse();
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException ao adicionar perfil nulo ou sem referência temporal")
        void deveRejeitarPerfilNuloOuAgoraNulo() {
            Usuario usuario = Usuario.criar(NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA);
            Perfil perfil = Perfil.criar(1, "ROLE_USUARIO", "Perfil");

            assertThatThrownBy(() -> usuario.adicionarPerfil(null, AGORA, usuario.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Perfil não pode ser nulo.");

            assertThatThrownBy(() -> usuario.adicionarPerfil(perfil, null, usuario.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para atribuição de perfil.");
        }
    }

    @Nested
    @DisplayName("atualizarDadosCadastrais(...)")
    class AtualizarDadosCadastrais {

        @Test
        @DisplayName("Deve atualizar todos os dados cadastrais informados e registrar auditoria")
        void deveAtualizarTodosOsCamposInformados() {
            Usuario usuario = Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO,
                    Genero.MULHER_CIS, RacaEtnia.BRANCA, OrientacaoSexual.HETEROSSEXUAL, AGORA
            );
            OffsetDateTime momentoAtualizacao = AGORA.plusDays(1);

            usuario.atualizarDadosCadastrais(
                    "  Maria da Silva Santos  ",
                    "  11999998888  ",
                    Genero.NAO_BINARIO,
                    RacaEtnia.PRETA,
                    OrientacaoSexual.PANSEXUAL,
                    momentoAtualizacao
            );

            assertThat(usuario.getNomeCompleto()).isEqualTo("Maria da Silva Santos");
            assertThat(usuario.getTelefone()).isEqualTo("11999998888");
            assertThat(usuario.getGenero()).isEqualTo(Genero.NAO_BINARIO);
            assertThat(usuario.getRacaEtnia()).isEqualTo(RacaEtnia.PRETA);
            assertThat(usuario.getOrientacaoSexual()).isEqualTo(OrientacaoSexual.PANSEXUAL);
            assertThat(usuario.getAtualizadoEm()).isEqualTo(momentoAtualizacao);
            assertThat(usuario.getAtualizadoPor()).isEqualTo(usuario.getId());
        }

        @Test
        @DisplayName("Deve preservar valores atuais quando parâmetros de atualização forem nulos (atualização parcial)")
        void devePreservarValoresQuandoParametrosNulos() {
            Usuario usuario = Usuario.criar(
                    NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO,
                    Genero.MULHER_CIS, RacaEtnia.PARDA, OrientacaoSexual.BISSEXUAL, AGORA
            );
            OffsetDateTime momentoAtualizacao = AGORA.plusHours(2);

            usuario.atualizarDadosCadastrais(null, null, null, null, null, momentoAtualizacao);

            assertThat(usuario.getNomeCompleto()).isEqualTo(NOME_VALIDO);
            assertThat(usuario.getTelefone()).isEqualTo(TELEFONE_VALIDO);
            assertThat(usuario.getGenero()).isEqualTo(Genero.MULHER_CIS);
            assertThat(usuario.getRacaEtnia()).isEqualTo(RacaEtnia.PARDA);
            assertThat(usuario.getOrientacaoSexual()).isEqualTo(OrientacaoSexual.BISSEXUAL);
            assertThat(usuario.getAtualizadoEm()).isEqualTo(momentoAtualizacao);
        }

        @Test
        @DisplayName("Deve validar nome, telefone e referência temporal na atualização cadastral")
        void deveValidarRegrasNaAtualizacaoCadastral() {
            Usuario usuario = Usuario.criar(NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA);

            assertThatThrownBy(() -> usuario.atualizarDadosCadastrais(
                    "Novo Nome", TELEFONE_VALIDO, null, null, null, null
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para atualização cadastral.");

            assertThatThrownBy(() -> usuario.atualizarDadosCadastrais(
                    "Ab", null, null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome completo deve ter entre 3 e 150 caracteres.");

            assertThatThrownBy(() -> usuario.atualizarDadosCadastrais(
                    null, "123", null, null, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Telefone deve conter entre 10 e 11 dígitos numéricos.");
        }
    }

    @Nested
    @DisplayName("validarContaAtiva()")
    class ValidarContaAtiva {

        @Test
        @DisplayName("Não deve lançar exceção quando conta estiver ativa")
        void naoDeveLancarExcecaoQuandoAtiva() {
            Usuario usuario = Usuario.criar(NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA);

            assertThatCode(usuario::validarContaAtiva).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar UnauthorizedException quando conta estiver inativa")
        void deveLancarUnauthorizedExceptionQuandoInativa() {
            Usuario usuario = Usuario.criar(NOME_VALIDO, CPF_VALIDO, CPF_HASH, TELEFONE_VALIDO, null, null, null, AGORA);
            ReflectionTestUtils.setField(usuario, "ativo", false);

            assertThatThrownBy(usuario::validarContaAtiva)
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Conta de usuário está inativa.");
        }
    }
}
