package br.com.lume.api.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.lume.api.identidade.command.CadastroUsuarioCommand;
import br.com.lume.api.identidade.command.LoginCommand;
import br.com.lume.api.identidade.command.RefreshTokenCommand;
import br.com.lume.api.identidade.domain.Credencial;
import br.com.lume.api.identidade.domain.Perfil;
import br.com.lume.api.identidade.domain.Usuario;
import br.com.lume.api.identidade.dto.TokenResultDto;
import br.com.lume.api.identidade.dto.UsuarioAutenticadoDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.repository.CredencialRepository;
import br.com.lume.api.identidade.repository.PerfilRepository;
import br.com.lume.api.identidade.repository.UsuarioRepository;
import br.com.lume.api.model.CadastroUsuarioRequest;
import br.com.lume.api.model.Genero;
import br.com.lume.api.model.OrientacaoSexual;
import br.com.lume.api.model.RacaEtnia;
import br.com.lume.api.shared.config.CryptoProperties;
import br.com.lume.api.shared.config.LockoutProperties;
import br.com.lume.api.shared.exception.AccountLockedException;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.exception.UnauthorizedException;
import br.com.lume.api.shared.security.CpfCryptoService;
import br.com.lume.api.shared.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoService - Testes Unitários")
class AutenticacaoServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);
    private static final String CPF_VALIDO = "52998224725";
    private static final String CPF_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String TELEFONE_VALIDO = "11987654321";
    private static final String EMAIL_VALIDO = "maria.silva@exemplo.com";
    private static final String SENHA_VALIDA = "SenhaForte#2026";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CredencialRepository credencialRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UsuarioMapper usuarioMapper;
    private LockoutProperties lockoutProperties;
    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void setUp() {
        lockoutProperties = new LockoutProperties(5, 15, 3);
        usuarioMapper = Mappers.getMapper(UsuarioMapper.class);
        CpfCryptoService cpfCryptoService = new CpfCryptoService(new CryptoProperties(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "0123456789abcdef0123456789abcdef"
        ));
        autenticacaoService = new AutenticacaoService(
                usuarioRepository,
                credencialRepository,
                perfilRepository,
                usuarioMapper,
                passwordEncoder,
                jwtService,
                cpfCryptoService,
                lockoutProperties,
                FIXED_CLOCK
        );
    }

    @Nested
    @DisplayName("cadastrar(CadastroUsuarioCommand)")
    class Cadastrar {

        @Test
        @DisplayName("Deve cadastrar novo usuário com perfil ROLE_USUARIO e credencial normalizada")
        void deveCadastrarUsuarioComSucesso() {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva",
                    CPF_VALIDO,
                    " 11987654321 ",
                    "  Maria.Silva@Exemplo.com ",
                    SENHA_VALIDA
            )
                    .genero(Genero.MULHER_CIS)
                    .racaEtnia(RacaEtnia.PARDA)
                    .orientacaoSexual(OrientacaoSexual.HETEROSSEXUAL);

            Perfil perfilPadrao = Perfil.criar(1, AutenticacaoService.ROLE_USUARIO, "Perfil cidadão");

            when(usuarioRepository.existsByCpfHash(any())).thenReturn(false);
            when(usuarioRepository.existsByTelefone(TELEFONE_VALIDO)).thenReturn(false);
            when(credencialRepository.existsByEmailIgnoreCase(EMAIL_VALIDO)).thenReturn(false);
            when(perfilRepository.findByNome(AutenticacaoService.ROLE_USUARIO)).thenReturn(Optional.of(perfilPadrao));
            when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("$2a$12$hashGerado");

            UsuarioAutenticadoDto response = autenticacaoService.cadastrar(usuarioMapper.toCommand(request));

            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            ArgumentCaptor<Credencial> credencialCaptor = ArgumentCaptor.forClass(Credencial.class);
            verify(usuarioRepository).save(usuarioCaptor.capture());
            verify(credencialRepository).save(credencialCaptor.capture());

            Usuario usuarioSalvo = usuarioCaptor.getValue();
            Credencial credencialSalva = credencialCaptor.getValue();

            assertThat(usuarioSalvo.getNomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(usuarioSalvo.getCpf()).isEqualTo(CPF_VALIDO);
            assertThat(usuarioSalvo.getTelefone()).isEqualTo(TELEFONE_VALIDO);
            assertThat(usuarioSalvo.getNomesPerfis()).containsExactly("ROLE_USUARIO");
            assertThat(credencialSalva.getEmail()).isEqualTo(EMAIL_VALIDO);
            assertThat(credencialSalva.getHashSenha()).isEqualTo("$2a$12$hashGerado");

            assertThat(response.id()).isEqualTo(usuarioSalvo.getId());
            assertThat(response.nomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(response.email()).isEqualTo(EMAIL_VALIDO);
            assertThat(response.perfis()).containsExactly("ROLE_USUARIO");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando request for nulo")
        void deveRejeitarRequestNulo() {
            assertThatThrownBy(() -> autenticacaoService.cadastrar((CadastroUsuarioCommand) null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Dados de cadastro são obrigatórios.");

            verifyNoInteractions(usuarioRepository, credencialRepository, perfilRepository);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando senha for nula, menor que 8 ou maior que 64 caracteres")
        void deveRejeitarSenhaComTamanhoInvalido() {
            CadastroUsuarioRequest reqSenhaNula = new CadastroUsuarioRequest(
                    "Maria", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, null
            );
            CadastroUsuarioRequest reqSenhaCurta = new CadastroUsuarioRequest(
                    "Maria", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, "1234567"
            );
            CadastroUsuarioRequest reqSenhaLonga = new CadastroUsuarioRequest(
                    "Maria", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, "a".repeat(65)
            );

            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(reqSenhaNula)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A senha deve conter entre 8 e 64 caracteres.");
            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(reqSenhaCurta)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A senha deve conter entre 8 e 64 caracteres.");
            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(reqSenhaLonga)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A senha deve conter entre 8 e 64 caracteres.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando e-mail for nulo ou em branco")
        void deveRejeitarEmailNuloOuEmBranco() {
            CadastroUsuarioRequest reqEmailVazio = new CadastroUsuarioRequest(
                    "Maria da Silva", CPF_VALIDO, TELEFONE_VALIDO, "   ", SENHA_VALIDA
            );

            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(reqEmailVazio)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("E-mail é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando CPF já estiver cadastrado")
        void deveLancarConflictExceptionQuandoCpfDuplicado() {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, SENHA_VALIDA
            );
            when(usuarioRepository.existsByCpfHash(any())).thenReturn(true);

            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(request)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Não foi possível concluir o cadastro com os dados informados.");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando telefone já estiver cadastrado")
        void deveLancarConflictExceptionQuandoTelefoneDuplicado() {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, SENHA_VALIDA
            );
            when(usuarioRepository.existsByCpfHash(any())).thenReturn(false);
            when(usuarioRepository.existsByTelefone(TELEFONE_VALIDO)).thenReturn(true);

            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(request)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Não foi possível concluir o cadastro com os dados informados.");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando e-mail já estiver cadastrado")
        void deveLancarConflictExceptionQuandoEmailDuplicado() {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, SENHA_VALIDA
            );
            when(usuarioRepository.existsByCpfHash(any())).thenReturn(false);
            when(usuarioRepository.existsByTelefone(TELEFONE_VALIDO)).thenReturn(false);
            when(credencialRepository.existsByEmailIgnoreCase(EMAIL_VALIDO)).thenReturn(true);

            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(request)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Não foi possível concluir o cadastro com os dados informados.");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando perfil padrão ROLE_USUARIO não existir")
        void deveLancarResourceNotFoundExceptionQuandoPerfilPadraoAusente() {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva", CPF_VALIDO, TELEFONE_VALIDO, EMAIL_VALIDO, SENHA_VALIDA
            );
            when(usuarioRepository.existsByCpfHash(any())).thenReturn(false);
            when(usuarioRepository.existsByTelefone(TELEFONE_VALIDO)).thenReturn(false);
            when(credencialRepository.existsByEmailIgnoreCase(EMAIL_VALIDO)).thenReturn(false);
            when(perfilRepository.findByNome(AutenticacaoService.ROLE_USUARIO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> autenticacaoService.cadastrar(usuarioMapper.toCommand(request)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Perfil padrão ROLE_USUARIO não encontrado.");

            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("autenticar(LoginCommand)")
    class Autenticar {

        @Test
        @DisplayName("Deve autenticar usuário com sucesso, resetar tentativas falhas e emitir tokens")
        void deveAutenticarComSucesso() {
            Usuario usuario = criarUsuarioComPerfil();
            Credencial credencial = Credencial.criar(usuario, EMAIL_VALIDO, "$2a$12$hashValido", AGORA.minusDays(1));
            credencial.registrarFalhaLogin(AGORA.minusMinutes(5), 5, 15, 3);

            when(credencialRepository.findByEmailWithUsuarioAndPerfis(EMAIL_VALIDO))
                    .thenReturn(Optional.of(credencial));
            when(passwordEncoder.matches(SENHA_VALIDA, "$2a$12$hashValido")).thenReturn(true);
            when(jwtService.gerarAccessToken(usuario.getId(), EMAIL_VALIDO, List.of("ROLE_USUARIO")))
                    .thenReturn("access-token-jwt");
            when(jwtService.gerarRefreshToken(usuario.getId(), EMAIL_VALIDO))
                    .thenReturn("refresh-token-jwt");
            when(jwtService.getExpirationSeconds()).thenReturn(900L);

            TokenResultDto response = autenticacaoService.autenticar(new LoginCommand("  MARIA.SILVA@EXEMPLO.COM ", SENHA_VALIDA));

            assertThat(response.accessToken()).isEqualTo("access-token-jwt");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-jwt");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900);
            assertThat(response.usuario().id()).isEqualTo(usuario.getId());
            assertThat(response.usuario().email()).isEqualTo(EMAIL_VALIDO);

            assertThat(credencial.getTentativasFalhas()).isZero();
            assertThat(credencial.getBloqueadoAte()).isNull();
            assertThat(credencial.getUltimoLoginEm()).isEqualTo(AGORA);
            verify(credencialRepository).save(credencial);
        }

        @Test
        @DisplayName("Deve lançar UnauthorizedException quando request, e-mail ou senha forem nulos ou em branco")
        void deveRejeitarCredenciaisNulas() {
            assertThatThrownBy(() -> autenticacaoService.autenticar((LoginCommand) null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credenciais inválidas.");
            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(null, SENHA_VALIDA)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credenciais inválidas.");
            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand("   ", SENHA_VALIDA)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credenciais inválidas.");
            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(EMAIL_VALIDO, null)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credenciais inválidas.");
        }

        @Test
        @DisplayName("Deve lançar UnauthorizedException e executar dummy hash para mitigar timing attack quando e-mail não existir")
        void deveLancarUnauthorizedQuandoEmailInexistente() {
            when(credencialRepository.findByEmailWithUsuarioAndPerfis(EMAIL_VALIDO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(EMAIL_VALIDO, SENHA_VALIDA)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credenciais inválidas.");

            verify(passwordEncoder).matches(SENHA_VALIDA, AutenticacaoService.DUMMY_PASSWORD_HASH);
        }

        @Test
        @DisplayName("Deve garantir que DUMMY_PASSWORD_HASH é um hash BCrypt válido")
        void deveGarantirQueDummyHashEValido() {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder realEncoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            boolean matches = realEncoder.matches("qualquerSenha", AutenticacaoService.DUMMY_PASSWORD_HASH);
            assertThat(matches).isFalse();
        }

        @Test
        @DisplayName("Deve lançar UnauthorizedException quando conta de usuário estiver inativa")
        void deveLancarUnauthorizedQuandoContaInativa() {
            Usuario usuario = criarUsuarioComPerfil();
            ReflectionTestUtils.setField(usuario, "ativo", false);
            Credencial credencial = Credencial.criar(usuario, EMAIL_VALIDO, "$2a$12$hash", AGORA);

            when(credencialRepository.findByEmailWithUsuarioAndPerfis(EMAIL_VALIDO))
                    .thenReturn(Optional.of(credencial));
            when(passwordEncoder.matches(SENHA_VALIDA, "$2a$12$hash")).thenReturn(true);

            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(EMAIL_VALIDO, SENHA_VALIDA)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Conta de usuário está inativa.");

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Deve lançar AccountLockedException quando credencial já estiver bloqueada antes de validar senha")
        void deveLancarAccountLockedQuandoJaBloqueada() {
            Usuario usuario = criarUsuarioComPerfil();
            Credencial credencial = Credencial.criar(usuario, EMAIL_VALIDO, "$2a$12$hash", AGORA);
            for (int i = 0; i < 5; i++) {
                credencial.registrarFalhaLogin(AGORA.minusMinutes(2), 5, 15, 3);
            }

            when(credencialRepository.findByEmailWithUsuarioAndPerfis(EMAIL_VALIDO))
                    .thenReturn(Optional.of(credencial));

            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(EMAIL_VALIDO, SENHA_VALIDA)))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("Conta temporariamente bloqueada por excesso de tentativas falhas.");

            verifyNoInteractions(passwordEncoder, jwtService);
        }

        @Test
        @DisplayName("Deve incrementar tentativa falha, salvar Credencial e lançar UnauthorizedException nas tentativas 1 a 4")
        void deveIncrementarFalhaELancarUnauthorizedNasTentativas1A4() {
            Usuario usuario = criarUsuarioComPerfil();
            Credencial credencial = Credencial.criar(usuario, EMAIL_VALIDO, "$2a$12$hash", AGORA);

            when(credencialRepository.findByEmailWithUsuarioAndPerfis(EMAIL_VALIDO))
                    .thenReturn(Optional.of(credencial));
            when(passwordEncoder.matches("SenhaErrada", "$2a$12$hash")).thenReturn(false);

            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(EMAIL_VALIDO, "SenhaErrada")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credenciais inválidas.");

            assertThat(credencial.getTentativasFalhas()).isEqualTo(1);
            assertThat(credencial.getBloqueadoAte()).isNull();
            verify(credencialRepository).save(credencial);
        }

        @Test
        @DisplayName("Deve bloquear conta, salvar Credencial e lançar AccountLockedException ao atingir a 5ª falha")
        void deveBloquearContaELancarAccountLockedNaQuintaFalha() {
            Usuario usuario = criarUsuarioComPerfil();
            Credencial credencial = Credencial.criar(usuario, EMAIL_VALIDO, "$2a$12$hash", AGORA);
            for (int i = 0; i < 4; i++) {
                credencial.registrarFalhaLogin(AGORA.minusMinutes(1), 5, 15, 3);
            }

            when(credencialRepository.findByEmailWithUsuarioAndPerfis(EMAIL_VALIDO))
                    .thenReturn(Optional.of(credencial));
            when(passwordEncoder.matches("SenhaErrada", "$2a$12$hash")).thenReturn(false);

            assertThatThrownBy(() -> autenticacaoService.autenticar(new LoginCommand(EMAIL_VALIDO, "SenhaErrada")))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("Conta temporariamente bloqueada por excesso de tentativas falhas.");

            assertThat(credencial.getTentativasFalhas()).isEqualTo(5);
            assertThat(credencial.getBloqueadoAte()).isEqualTo(AGORA.plusMinutes(15));
            verify(credencialRepository).save(credencial);
        }
    }

    @Nested
    @DisplayName("renovarToken(RefreshTokenCommand)")
    class RenovarToken {

        @Test
        @DisplayName("Deve renovar token emitindo novos accessToken e refreshToken quando dados forem válidos")
        void deveRenovarTokenComSucesso() {
            Usuario usuario = criarUsuarioComPerfil();
            Credencial credencial = Credencial.criar(usuario, EMAIL_VALIDO, "$2a$12$hash", AGORA);

            when(jwtService.validarRefreshToken("valid-refresh-token")).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(credencial));
            when(jwtService.gerarAccessToken(eq(usuario.getId()), eq(EMAIL_VALIDO), eq(List.of("ROLE_USUARIO"))))
                    .thenReturn("novo-access-token");
            when(jwtService.gerarRefreshToken(usuario.getId(), EMAIL_VALIDO))
                    .thenReturn("novo-refresh-token");
            when(jwtService.getExpirationSeconds()).thenReturn(900L);

            TokenResultDto response = autenticacaoService.renovarToken(new RefreshTokenCommand("valid-refresh-token"));

            assertThat(response.accessToken()).isEqualTo("novo-access-token");
            assertThat(response.refreshToken()).isEqualTo("novo-refresh-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900);
            assertThat(response.usuario().id()).isEqualTo(usuario.getId());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar UnauthorizedException quando RefreshTokenCommand ou refreshToken for nulo ou em branco")
        void deveRejeitarRefreshTokenAusente(String tokenInvalido) {
            assertThatThrownBy(() -> autenticacaoService.renovarToken((RefreshTokenCommand) null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token é obrigatório.");

            assertThatThrownBy(() -> autenticacaoService.renovarToken(new RefreshTokenCommand(tokenInvalido)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar UnauthorizedException quando usuário ou credencial não forem encontrados")
        void deveRejeitarQuandoUsuarioOuCredencialNaoEncontrados() {
            UUID usuarioId = UUID.randomUUID();
            Usuario usuario = criarUsuarioComPerfil();

            when(jwtService.validarRefreshToken("token-sem-usuario")).thenReturn(usuarioId);
            when(usuarioRepository.findByIdWithPerfis(usuarioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> autenticacaoService.renovarToken(new RefreshTokenCommand("token-sem-usuario")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Usuário não encontrado para renovação de token.");

            when(jwtService.validarRefreshToken("token-sem-credencial")).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> autenticacaoService.renovarToken(new RefreshTokenCommand("token-sem-credencial")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Credencial não encontrada para renovação de token.");
        }

        @Test
        @DisplayName("Deve rejeitar renovação quando conta estiver inativa ou credencial bloqueada")
        void deveRejeitarQuandoContaInativaOuCredencialBloqueada() {
            Usuario usuarioInativo = criarUsuarioComPerfil();
            ReflectionTestUtils.setField(usuarioInativo, "ativo", false);
            Credencial credencial = Credencial.criar(usuarioInativo, EMAIL_VALIDO, "$2a$12$hash", AGORA);

            when(jwtService.validarRefreshToken("token-inativo")).thenReturn(usuarioInativo.getId());
            when(usuarioRepository.findByIdWithPerfis(usuarioInativo.getId())).thenReturn(Optional.of(usuarioInativo));
            when(credencialRepository.findByUsuarioId(usuarioInativo.getId())).thenReturn(Optional.of(credencial));

            assertThatThrownBy(() -> autenticacaoService.renovarToken(new RefreshTokenCommand("token-inativo")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Conta de usuário está inativa.");

            Usuario usuarioAtivo = criarUsuarioComPerfil();
            Credencial credencialBloqueada = Credencial.criar(usuarioAtivo, EMAIL_VALIDO, "$2a$12$hash", AGORA);
            for (int i = 0; i < 5; i++) {
                credencialBloqueada.registrarFalhaLogin(AGORA, 5, 15, 3);
            }

            when(jwtService.validarRefreshToken("token-bloqueado")).thenReturn(usuarioAtivo.getId());
            when(usuarioRepository.findByIdWithPerfis(usuarioAtivo.getId())).thenReturn(Optional.of(usuarioAtivo));
            when(credencialRepository.findByUsuarioId(usuarioAtivo.getId())).thenReturn(Optional.of(credencialBloqueada));

            assertThatThrownBy(() -> autenticacaoService.renovarToken(new RefreshTokenCommand("token-bloqueado")))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("Conta temporariamente bloqueada por excesso de tentativas falhas.");
        }
    }

    private Usuario criarUsuarioComPerfil() {
        Usuario usuario = Usuario.criar(
                "Maria da Silva",
                CPF_VALIDO,
                CPF_HASH,
                TELEFONE_VALIDO,
                br.com.lume.api.identidade.domain.Genero.MULHER_CIS,
                br.com.lume.api.identidade.domain.RacaEtnia.PARDA,
                br.com.lume.api.identidade.domain.OrientacaoSexual.HETEROSSEXUAL,
                AGORA
        );
        Perfil perfil = Perfil.criar(1, AutenticacaoService.ROLE_USUARIO, "Perfil cidadão");
        usuario.adicionarPerfil(perfil, AGORA, usuario.getId());
        return usuario;
    }
}
