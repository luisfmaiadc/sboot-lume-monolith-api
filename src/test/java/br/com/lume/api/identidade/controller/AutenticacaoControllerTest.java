package br.com.lume.api.identidade.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lume.api.identidade.command.CadastroUsuarioCommand;
import br.com.lume.api.identidade.command.LoginCommand;
import br.com.lume.api.identidade.command.RefreshTokenCommand;
import br.com.lume.api.identidade.dto.TokenResultDto;
import br.com.lume.api.identidade.dto.UsuarioAutenticadoDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.service.AutenticacaoService;
import br.com.lume.api.model.CadastroUsuarioRequest;
import br.com.lume.api.model.LoginRequest;
import br.com.lume.api.model.RefreshTokenRequest;
import br.com.lume.api.shared.exception.AccountLockedException;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.GlobalExceptionHandler;
import br.com.lume.api.shared.exception.UnauthorizedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoController - Testes Unitários (Standalone MockMvc)")
class AutenticacaoControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final UUID USUARIO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private AutenticacaoService autenticacaoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        UsuarioMapper usuarioMapper = Mappers.getMapper(UsuarioMapper.class);
        AutenticacaoController controller = new AutenticacaoController(autenticacaoService, usuarioMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(FIXED_CLOCK))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Nested
    @DisplayName("POST /v1/auth/cadastrar")
    class CadastrarUsuario {

        @Test
        @DisplayName("Deve retornar 201 Created com cabeçalho Location e resumo do usuário")
        void deveRetornar201CreatedNoCadastro() throws Exception {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva",
                    "52998224725",
                    "11987654321",
                    "maria.silva@exemplo.com",
                    "SenhaForte#2026"
            );
            UsuarioAutenticadoDto response = new UsuarioAutenticadoDto(
                    USUARIO_ID,
                    "Maria da Silva",
                    "maria.silva@exemplo.com",
                    List.of("ROLE_USUARIO")
            );
            when(autenticacaoService.cadastrar(any(CadastroUsuarioCommand.class))).thenReturn(response);

            mockMvc.perform(post("/v1/auth/cadastrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/usuarios/" + USUARIO_ID))
                    .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                    .andExpect(jsonPath("$.nomeCompleto").value("Maria da Silva"))
                    .andExpect(jsonPath("$.email").value("maria.silva@exemplo.com"))
                    .andExpect(jsonPath("$.perfis[0]").value("ROLE_USUARIO"));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request com lista de fields quando payload violar Bean Validation")
        void deveRetornar400QuandoPayloadInvalido() throws Exception {
            CadastroUsuarioRequest requestInvalido = new CadastroUsuarioRequest(
                    "Ma",
                    "123",
                    "123",
                    "email-invalido",
                    "curta"
            );

            mockMvc.perform(post("/v1/auth/cadastrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fields").isNotEmpty());
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando houver conflito de unicidade")
        void deveRetornar409QuandoConflitoDeUnicidade() throws Exception {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva",
                    "52998224725",
                    "11987654321",
                    "maria.silva@exemplo.com",
                    "SenhaForte#2026"
            );
            when(autenticacaoService.cadastrar(any(CadastroUsuarioCommand.class)))
                    .thenThrow(new ConflictException("Não foi possível concluir o cadastro com os dados informados."));

            mockMvc.perform(post("/v1/auth/cadastrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Não foi possível concluir o cadastro com os dados informados."));
        }

        @Test
        @DisplayName("Deve retornar 422 Unprocessable Content quando regra de negócio for violada")
        void deveRetornar422QuandoRegraDeNegocioViolada() throws Exception {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva",
                    "11111111111",
                    "11987654321",
                    "maria.silva@exemplo.com",
                    "SenhaForte#2026"
            );
            when(autenticacaoService.cadastrar(any(CadastroUsuarioCommand.class)))
                    .thenThrow(new BusinessRuleException("CPF informado é inválido."));

            mockMvc.perform(post("/v1/auth/cadastrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.message").value("CPF informado é inválido."));
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/login")
    class AutenticarUsuario {

        @Test
        @DisplayName("Deve retornar 200 OK com TokenResponse ao autenticar com sucesso")
        void deveRetornar200OkNoLogin() throws Exception {
            LoginRequest request = new LoginRequest("maria.silva@exemplo.com", "SenhaForte#2026");
            TokenResultDto tokenResult = criarTokenResultDto();
            when(autenticacaoService.autenticar(any(LoginCommand.class))).thenReturn(tokenResult);

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-jwt"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(jsonPath("$.usuario.id").value(USUARIO_ID.toString()));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando credenciais forem inválidas")
        void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
            LoginRequest request = new LoginRequest("maria.silva@exemplo.com", "SenhaErrada");
            when(autenticacaoService.autenticar(any(LoginCommand.class)))
                    .thenThrow(new UnauthorizedException("Credenciais inválidas."));

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Credenciais inválidas."));
        }

        @Test
        @DisplayName("Deve retornar 423 Locked quando conta estiver temporariamente bloqueada")
        void deveRetornar423QuandoContaBloqueada() throws Exception {
            LoginRequest request = new LoginRequest("maria.silva@exemplo.com", "SenhaErrada");
            when(autenticacaoService.autenticar(any(LoginCommand.class)))
                    .thenThrow(new AccountLockedException("Conta temporariamente bloqueada por excesso de tentativas falhas."));

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.status").value(423))
                    .andExpect(jsonPath("$.message").value("Conta temporariamente bloqueada por excesso de tentativas falhas."));
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/refresh")
    class RenovarToken {

        @Test
        @DisplayName("Deve retornar 200 OK com novos tokens ao renovar com sucesso")
        void deveRetornar200OkNoRefresh() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-valido");
            TokenResultDto tokenResult = criarTokenResultDto();
            when(autenticacaoService.renovarToken(any(RefreshTokenCommand.class))).thenReturn(tokenResult);

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-jwt"));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando refreshToken for inválido ou expirado")
        void deveRetornar401QuandoRefreshTokenInvalido() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-invalido");
            when(autenticacaoService.renovarToken(any(RefreshTokenCommand.class)))
                    .thenThrow(new UnauthorizedException("Token JWT inválido ou expirado."));

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Token JWT inválido ou expirado."));
        }
    }

    private TokenResultDto criarTokenResultDto() {
        UsuarioAutenticadoDto resumo = new UsuarioAutenticadoDto(
                USUARIO_ID,
                "Maria da Silva",
                "maria.silva@exemplo.com",
                List.of("ROLE_USUARIO")
        );
        return new TokenResultDto("access-jwt", "refresh-jwt", "Bearer", 900, resumo);
    }
}
