package br.com.lume.api.estabelecimento.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.estabelecimento.command.AtualizacaoEstabelecimentoCommand;
import br.com.lume.api.estabelecimento.command.CriacaoEstabelecimentoCommand;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoDetalheDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoPageDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.estabelecimento.mapper.EstabelecimentoMapper;
import br.com.lume.api.estabelecimento.service.EstabelecimentoService;
import br.com.lume.api.model.AtualizacaoEstabelecimentoRequest;
import br.com.lume.api.model.CriacaoEstabelecimentoRequest;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ForbiddenOperationException;
import br.com.lume.api.shared.exception.GlobalExceptionHandler;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
@DisplayName("EstabelecimentoController - Testes Unitários (Standalone MockMvc)")
class EstabelecimentoControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @Mock
    private EstabelecimentoService estabelecimentoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        EstabelecimentoMapper mapper = Mappers.getMapper(EstabelecimentoMapper.class);
        EstabelecimentoController controller = new EstabelecimentoController(estabelecimentoService, mapper);
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
    @DisplayName("GET /v1/estabelecimentos/autocomplete")
    class Autocomplete {

        @Test
        @DisplayName("Deve retornar 200 OK com lista de sugestões de estabelecimentos")
        void deveRetornar200NoAutocomplete() throws Exception {
            EstabelecimentoResumoDto item = new EstabelecimentoResumoDto(
                    ESTABELECIMENTO_ID,
                    1,
                    "Bares e Restaurantes",
                    "Bar do Zé",
                    "Rua Augusta",
                    "1200",
                    "Consolação",
                    "São Paulo",
                    "SP",
                    -23.55052,
                    -46.633308,
                    new BigDecimal("4.85"),
                    true,
                    true
            );
            when(estabelecimentoService.autocompleteEstabelecimentos("Bar", "São Paulo", 5))
                    .thenReturn(List.of(item));

            mockMvc.perform(get("/v1/estabelecimentos/autocomplete")
                            .param("termo", "Bar")
                            .param("cidade", "São Paulo")
                            .param("limite", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(ESTABELECIMENTO_ID.toString()))
                    .andExpect(jsonPath("$[0].nome").value("Bar do Zé"))
                    .andExpect(jsonPath("$[0].categoriaNome").value("Bares e Restaurantes"))
                    .andExpect(jsonPath("$[0].scoreReputacao").value(4.85));
        }
    }

    @Nested
    @DisplayName("GET /v1/estabelecimentos")
    class ListarEstabelecimentos {

        @Test
        @DisplayName("Deve retornar 200 OK com página de estabelecimentos")
        void deveRetornar200NaListagem() throws Exception {
            EstabelecimentoResumoDto resumo = new EstabelecimentoResumoDto(
                    ESTABELECIMENTO_ID,
                    1,
                    "Bares e Restaurantes",
                    "Bar do Zé",
                    "Rua Augusta",
                    "1200",
                    "Consolação",
                    "São Paulo",
                    "SP",
                    -23.55052,
                    -46.633308,
                    new BigDecimal("5.00"),
                    false,
                    true
            );
            EstabelecimentoPageDto pageResult = new EstabelecimentoPageDto(
                    List.of(resumo), 1L, 1, 0, 20
            );
            when(estabelecimentoService.listarEstabelecimentos(
                    "Bar", 1, "São Paulo", "Consolação", null, null, 1000, 0, 20
            )).thenReturn(pageResult);

            mockMvc.perform(get("/v1/estabelecimentos")
                            .param("termo", "Bar")
                            .param("categoriaId", "1")
                            .param("cidade", "São Paulo")
                            .param("bairro", "Consolação")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(ESTABELECIMENTO_ID.toString()));
        }

        @Test
        @DisplayName("Deve retornar 422 Unprocessable Content quando apenas latitude for informada (XOR)")
        void deveRetornar422QuandoXorCoordenadasInvalido() throws Exception {
            when(estabelecimentoService.listarEstabelecimentos(
                    null, null, null, null, -23.55052, null, 1000, 0, 20
            )).thenThrow(new BusinessRuleException("Latitude e longitude devem ser informadas em conjunto para busca geográfica."));

            mockMvc.perform(get("/v1/estabelecimentos")
                            .param("latitude", "-23.55052")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.status").value(422));
        }
    }

    @Nested
    @DisplayName("POST /v1/estabelecimentos")
    class CadastrarEstabelecimento {

        @Test
        @DisplayName("Deve retornar 201 Created com header Location e detalhe do estabelecimento")
        void deveRetornar201NoCadastro() throws Exception {
            CriacaoEstabelecimentoRequest request = criarRequestValido();
            EstabelecimentoDetalheDto detalhe = criarDetalheDto();
            when(estabelecimentoService.cadastrarEstabelecimento(any(CriacaoEstabelecimentoCommand.class)))
                    .thenReturn(detalhe);

            mockMvc.perform(post("/v1/estabelecimentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/estabelecimentos/" + ESTABELECIMENTO_ID))
                    .andExpect(jsonPath("$.id").value(ESTABELECIMENTO_ID.toString()))
                    .andExpect(jsonPath("$.nome").value("Bar do Zé"))
                    .andExpect(jsonPath("$.scoreReputacao").value(5.0));
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando CNPJ já estiver cadastrado")
        void deveRetornar409NoCadastroComCnpjDuplicado() throws Exception {
            CriacaoEstabelecimentoRequest request = criarRequestValido();
            when(estabelecimentoService.cadastrarEstabelecimento(any(CriacaoEstabelecimentoCommand.class)))
                    .thenThrow(new ConflictException("Já existe um estabelecimento cadastrado com o CNPJ informado."));

            mockMvc.perform(post("/v1/estabelecimentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("GET /v1/estabelecimentos/meu")
    class ObterMeuEstabelecimento {

        @Test
        @DisplayName("Deve retornar 200 OK quando gestor possuir estabelecimento vinculado")
        void deveRetornar200QuandoGestorTiverEstabelecimento() throws Exception {
            when(estabelecimentoService.obterMeuEstabelecimento()).thenReturn(Optional.of(criarDetalheDto()));

            mockMvc.perform(get("/v1/estabelecimentos/meu")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ESTABELECIMENTO_ID.toString()));
        }

        @Test
        @DisplayName("Deve retornar 204 No Content quando gestor não possuir estabelecimento vinculado")
        void deveRetornar204QuandoGestorSemEstabelecimento() throws Exception {
            when(estabelecimentoService.obterMeuEstabelecimento()).thenReturn(Optional.empty());

            mockMvc.perform(get("/v1/estabelecimentos/meu")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 403 Forbidden quando usuário não possuir ROLE_GESTOR")
        void deveRetornar403QuandoSemRoleGestor() throws Exception {
            when(estabelecimentoService.obterMeuEstabelecimento())
                    .thenThrow(new ForbiddenOperationException("Acesso restrito a usuários com perfil de gestor."));

            mockMvc.perform(get("/v1/estabelecimentos/meu")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Nested
    @DisplayName("GET /v1/estabelecimentos/{id}, PUT /v1/estabelecimentos/{id} e GET /v1/estabelecimentos/{id}/ocorrencias")
    class OperacoesPorId {

        @Test
        @DisplayName("Deve retornar 200 OK ao obter estabelecimento por ID e 404 Not Found quando inexistente")
        void deveObterEstabelecimentoPorIdOu404() throws Exception {
            when(estabelecimentoService.obterEstabelecimentoPorId(ESTABELECIMENTO_ID))
                    .thenReturn(criarDetalheDto());

            mockMvc.perform(get("/v1/estabelecimentos/{id}", ESTABELECIMENTO_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ESTABELECIMENTO_ID.toString()));

            UUID idInexistente = UUID.randomUUID();
            when(estabelecimentoService.obterEstabelecimentoPorId(idInexistente))
                    .thenThrow(new ResourceNotFoundException("Estabelecimento não encontrado."));

            mockMvc.perform(get("/v1/estabelecimentos/{id}", idInexistente)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Deve retornar 200 OK ao atualizar estabelecimento e 403 Forbidden quando não autorizado")
        void deveAtualizarEstabelecimentoOuRetornar403() throws Exception {
            AtualizacaoEstabelecimentoRequest request = new AtualizacaoEstabelecimentoRequest()
                    .nome("Bar do Zé Atualizado")
                    .ativo(true);

            when(estabelecimentoService.atualizarEstabelecimento(
                    eq(ESTABELECIMENTO_ID), any(AtualizacaoEstabelecimentoCommand.class)
            )).thenReturn(criarDetalheDto());

            mockMvc.perform(put("/v1/estabelecimentos/{id}", ESTABELECIMENTO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ESTABELECIMENTO_ID.toString()));

            when(estabelecimentoService.atualizarEstabelecimento(
                    eq(ESTABELECIMENTO_ID), any(AtualizacaoEstabelecimentoCommand.class)
            )).thenThrow(new ForbiddenOperationException("Usuário não possui vínculo ativo de gestão para este estabelecimento."));

            mockMvc.perform(put("/v1/estabelecimentos/{id}", ESTABELECIMENTO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Deve retornar 200 OK ao listar ocorrências do estabelecimento")
        void deveListarOcorrenciasDoEstabelecimento() throws Exception {
            OcorrenciaResumoPagePortDto pageResult = new OcorrenciaResumoPagePortDto(List.of(), 0L, 0, 0, 20);
            when(estabelecimentoService.listarOcorrenciasEstabelecimento(
                    ESTABELECIMENTO_ID, null, null, null, 0, 20
            )).thenReturn(pageResult);

            mockMvc.perform(get("/v1/estabelecimentos/{id}/ocorrencias", ESTABELECIMENTO_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    private CriacaoEstabelecimentoRequest criarRequestValido() {
        return new CriacaoEstabelecimentoRequest(
                "Bar do Zé",
                1,
                "Rua Augusta",
                "Consolação",
                "São Paulo",
                "SP",
                -23.55052,
                -46.633308
        )
                .cnpj("11222333000181")
                .numero("1200");
    }

    private EstabelecimentoDetalheDto criarDetalheDto() {
        CategoriaEstabelecimentoDto categoria = new CategoriaEstabelecimentoDto(
                1, "BAR_RESTAURANTE", "Bares e Restaurantes", true
        );
        return new EstabelecimentoDetalheDto(
                ESTABELECIMENTO_ID,
                "Bar do Zé",
                categoria,
                "11222333000181",
                "Rua Augusta",
                "1200",
                "Consolação",
                "São Paulo",
                "SP",
                -23.55052,
                -46.633308,
                new BigDecimal("5.00"),
                false,
                true,
                0,
                new DistribuicaoTurnoDto(0, 0, 0, 0),
                List.of()
        );
    }
}
