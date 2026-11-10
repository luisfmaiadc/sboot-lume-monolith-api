package br.com.lume.api.identidade.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lume.api.identidade.command.AtualizacaoUsuarioCommand;
import br.com.lume.api.identidade.dto.UsuarioDetalheDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.service.UsuarioService;
import br.com.lume.api.model.AtualizacaoUsuarioRequest;
import br.com.lume.api.model.Genero;
import br.com.lume.api.model.OrientacaoSexual;
import br.com.lume.api.model.RacaEtnia;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.GlobalExceptionHandler;
import br.com.lume.api.shared.exception.UnauthorizedException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
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
@DisplayName("UsuarioController - Testes Unitários (Standalone MockMvc)")
class UsuarioControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);
    private static final UUID USUARIO_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        UsuarioController controller = new UsuarioController(usuarioService, Mappers.getMapper(UsuarioMapper.class));
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
    @DisplayName("GET /v1/usuarios/me")
    class ObterPerfilUsuarioLogado {

        @Test
        @DisplayName("Deve retornar 200 OK com dados detalhados do perfil autenticado")
        void deveRetornar200OkComPerfilLogado() throws Exception {
            UsuarioDetalheDto response = criarDetalheDto();
            when(usuarioService.obterPerfilLogado()).thenReturn(response);

            mockMvc.perform(get("/v1/usuarios/me")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                    .andExpect(jsonPath("$.nomeCompleto").value("Maria da Silva"))
                    .andExpect(jsonPath("$.cpf").value("52998224725"))
                    .andExpect(jsonPath("$.telefone").value("11987654321"))
                    .andExpect(jsonPath("$.email").value("maria.silva@exemplo.com"))
                    .andExpect(jsonPath("$.genero").value("MULHER_CIS"))
                    .andExpect(jsonPath("$.racaEtnia").value("PARDA"))
                    .andExpect(jsonPath("$.orientacaoSexual").value("BISSEXUAL"))
                    .andExpect(jsonPath("$.perfis[0]").value("ROLE_USUARIO"));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando usuário não estiver autenticado")
        void deveRetornar401QuandoNaoAutenticado() throws Exception {
            when(usuarioService.obterPerfilLogado())
                    .thenThrow(new UnauthorizedException("Usuário não está autenticado no contexto atual."));

            mockMvc.perform(get("/v1/usuarios/me")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Usuário não está autenticado no contexto atual."));
        }
    }

    @Nested
    @DisplayName("PUT /v1/usuarios/me")
    class AtualizarPerfilUsuarioLogado {

        @Test
        @DisplayName("Deve retornar 200 OK ao atualizar perfil do usuário logado com sucesso")
        void deveRetornar200OkNaAtualizacao() throws Exception {
            AtualizacaoUsuarioRequest request = new AtualizacaoUsuarioRequest()
                    .nomeCompleto("Maria da Silva")
                    .telefone("11987654321")
                    .genero(Genero.MULHER_CIS)
                    .racaEtnia(RacaEtnia.PARDA)
                    .orientacaoSexual(OrientacaoSexual.BISSEXUAL);

            when(usuarioService.atualizarPerfilLogado(any(AtualizacaoUsuarioCommand.class)))
                    .thenReturn(criarDetalheDto());

            mockMvc.perform(put("/v1/usuarios/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                    .andExpect(jsonPath("$.nomeCompleto").value("Maria da Silva"))
                    .andExpect(jsonPath("$.telefone").value("11987654321"));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando dados de atualização violarem Bean Validation")
        void deveRetornar400QuandoDadosInvalidos() throws Exception {
            AtualizacaoUsuarioRequest requestInvalido = new AtualizacaoUsuarioRequest()
                    .nomeCompleto("Ab")
                    .telefone("123");

            mockMvc.perform(put("/v1/usuarios/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fields").isNotEmpty());
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando telefone já pertencer a outro usuário")
        void deveRetornar409QuandoConflitoDeTelefone() throws Exception {
            AtualizacaoUsuarioRequest request = new AtualizacaoUsuarioRequest()
                    .nomeCompleto("Maria da Silva")
                    .telefone("11999990000");

            when(usuarioService.atualizarPerfilLogado(any(AtualizacaoUsuarioCommand.class)))
                    .thenThrow(new ConflictException("Já existe outro usuário cadastrado com o telefone informado."));

            mockMvc.perform(put("/v1/usuarios/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Já existe outro usuário cadastrado com o telefone informado."));
        }
    }

    private UsuarioDetalheDto criarDetalheDto() {
        return new UsuarioDetalheDto(
                USUARIO_ID,
                "Maria da Silva",
                "52998224725",
                "11987654321",
                "maria.silva@exemplo.com",
                br.com.lume.api.identidade.domain.Genero.MULHER_CIS,
                br.com.lume.api.identidade.domain.RacaEtnia.PARDA,
                br.com.lume.api.identidade.domain.OrientacaoSexual.BISSEXUAL,
                List.of("ROLE_USUARIO"),
                true,
                AGORA
        );
    }
}
