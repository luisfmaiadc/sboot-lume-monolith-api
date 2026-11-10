package br.com.lume.api.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lume.api.catalogo.controller.CatalogoController;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.catalogo.service.CatalogoService;
import br.com.lume.api.estabelecimento.controller.EstabelecimentoController;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoDetalheDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoPageDto;
import br.com.lume.api.estabelecimento.service.EstabelecimentoService;
import br.com.lume.api.identidade.controller.AutenticacaoController;
import br.com.lume.api.identidade.controller.UsuarioController;
import br.com.lume.api.identidade.domain.Genero;
import br.com.lume.api.identidade.domain.OrientacaoSexual;
import br.com.lume.api.identidade.domain.RacaEtnia;
import br.com.lume.api.identidade.dto.TokenResultDto;
import br.com.lume.api.identidade.dto.UsuarioAutenticadoDto;
import br.com.lume.api.identidade.dto.UsuarioDetalheDto;
import br.com.lume.api.identidade.service.AutenticacaoService;
import br.com.lume.api.identidade.service.UsuarioService;
import br.com.lume.api.model.AtualizacaoStatusOcorrenciaRequest;
import br.com.lume.api.model.AtualizacaoUsuarioRequest;
import br.com.lume.api.model.CadastroUsuarioRequest;
import br.com.lume.api.model.CriacaoEstabelecimentoRequest;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.LoginRequest;
import br.com.lume.api.model.RefreshTokenRequest;
import br.com.lume.api.model.RegistroOcorrenciaRequest;
import br.com.lume.api.model.TipoAmbiente;
import br.com.lume.api.model.TipoValidacao;
import br.com.lume.api.model.ValidacaoOcorrenciaRequest;
import br.com.lume.api.ocorrencia.controller.OcorrenciaController;
import br.com.lume.api.ocorrencia.controller.SegurancaController;
import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.ocorrencia.dto.DiagnosticoSegurancaDto;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaPageDto;
import br.com.lume.api.ocorrencia.dto.OcorrenciaDetalheDto;
import br.com.lume.api.ocorrencia.dto.PontoMapaDto;
import br.com.lume.api.ocorrencia.service.OcorrenciaService;
import br.com.lume.api.ocorrencia.service.SegurancaTerritorialService;
import br.com.lume.api.shared.config.JwtProperties;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import br.com.lume.api.shared.web.RequestIdFilter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {
        CatalogoController.class,
        AutenticacaoController.class,
        UsuarioController.class,
        EstabelecimentoController.class,
        OcorrenciaController.class,
        SegurancaController.class
})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        RequestIdFilter.class,
        WebMvcSecurityTestConfig.class
})
@ActiveProfiles("test")
@DisplayName("SecurityFilterChain & WebMvc - Testes Integrados de Camada Web e Segurança")
class SecurityFilterChainWebMvcTest {

    private static final Instant FIXED_INSTANT = WebMvcSecurityTestConfig.FIXED_INSTANT;
    private static final Clock FIXED_CLOCK = WebMvcSecurityTestConfig.FIXED_CLOCK;
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);

    private static final UUID USUARIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OCORRENCIA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @MockitoBean
    private CatalogoService catalogoService;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private EstabelecimentoService estabelecimentoService;

    @MockitoBean
    private OcorrenciaService ocorrenciaService;

    @MockitoBean
    private SegurancaTerritorialService segurancaTerritorialService;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Autowired
    SecurityFilterChainWebMvcTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    private String gerarBearerToken(String... roles) {
        return "Bearer " + jwtService.gerarAccessToken(USUARIO_ID, "usuario@lume.com.br", List.of(roles));
    }

    private OcorrenciaDetalheDto criarOcorrenciaDetalheDto() {
        TipoViolenciaDto tv = new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, true);
        return new OcorrenciaDetalheDto(
                OCORRENCIA_ID,
                null,
                null,
                -23.551,
                -46.633,
                null,
                null,
                "Consolação",
                "São Paulo",
                "SP",
                LocalDate.of(2026, 11, 1),
                br.com.lume.api.ocorrencia.domain.FaixaHorario.NOITE,
                tv,
                null,
                br.com.lume.api.ocorrencia.domain.TipoAmbiente.VIA_PUBLICA,
                2,
                LocalDate.of(2026, 11, 8)
        );
    }

    private EstabelecimentoDetalheDto criarEstabelecimentoDetalheDto() {
        return new EstabelecimentoDetalheDto(
                ESTABELECIMENTO_ID,
                "Café Aurora",
                null,
                null,
                "Rua Augusta",
                "100",
                "Consolação",
                "São Paulo",
                "SP",
                -23.551,
                -46.633,
                new BigDecimal("4.50"),
                true,
                true,
                0,
                new DistribuicaoTurnoDto(0, 0, 0, 0),
                List.of()
        );
    }

    private RegistroOcorrenciaRequest criarRegistroOcorrenciaValido() {
        RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                LocalDate.of(2026, 11, 8),
                FaixaHorario.NOITE,
                1
        );
        request.setLatitude(-23.551);
        request.setLongitude(-46.633);
        request.setLogradouro("Rua Augusta");
        request.setBairro("Consolação");
        request.setCidade("São Paulo");
        request.setEstado("SP");
        request.setTipoAmbiente(TipoAmbiente.VIA_PUBLICA);
        return request;
    }

    @Nested
    @DisplayName("Rotas Públicas (permitAll)")
    class RotasPublicas {

        @Test
        @DisplayName("Deve permitir POST /v1/auth/cadastrar sem autenticação e retornar 201 com Location")
        void devePermitirCadastroSemToken() throws Exception {
            CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                    "Maria da Silva",
                    "52998224725",
                    "11987654321",
                    "maria.silva@exemplo.com",
                    "SenhaForte#2026"
            );
            when(autenticacaoService.cadastrar(any())).thenReturn(
                    new UsuarioAutenticadoDto(USUARIO_ID, "Maria da Silva", "maria.silva@exemplo.com", List.of("ROLE_USUARIO"))
            );

            mockMvc.perform(post("/v1/auth/cadastrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/v1/usuarios/" + USUARIO_ID))
                    .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()));
        }

        @Test
        @DisplayName("Deve permitir POST /v1/auth/login e /v1/auth/refresh sem autenticação")
        void devePermitirLoginERefreshSemToken() throws Exception {
            UsuarioAutenticadoDto usuario = new UsuarioAutenticadoDto(
                    USUARIO_ID, "Maria da Silva", "maria@exemplo.com", List.of("ROLE_USUARIO")
            );
            TokenResultDto tokenResult = new TokenResultDto("access-jwt", "refresh-jwt", "Bearer", 900, usuario);
            when(autenticacaoService.autenticar(any())).thenReturn(tokenResult);
            when(autenticacaoService.renovarToken(any())).thenReturn(tokenResult);

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("maria@exemplo.com", "SenhaForte#2026"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-jwt"));

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refresh-jwt"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refreshToken").value("refresh-jwt"));
        }

        @Test
        @DisplayName("Deve permitir acesso público aos endpoints de catálogos")
        void devePermitirCatalogosSemToken() throws Exception {
            when(catalogoService.listarTiposViolenciaAtivos()).thenReturn(List.of());
            when(catalogoService.listarMarcadoresVulnerabilidadeAtivos()).thenReturn(List.of());
            when(catalogoService.listarCategoriasEstabelecimentosAtivas()).thenReturn(List.of());

            mockMvc.perform(get("/v1/catalogos/tipos-violencia"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, Matchers.containsString("max-age=3600")));
            mockMvc.perform(get("/v1/catalogos/marcadores-vulnerabilidade")).andExpect(status().isOk());
            mockMvc.perform(get("/v1/catalogos/categorias-estabelecimentos")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve permitir consultas públicas de segurança territorial e ocorrências (RN01)")
        void devePermitirSegurancaEOcorrenciaDetalheSemToken() throws Exception {
            when(segurancaTerritorialService.obterDiagnosticoRaio(-23.55, -46.63, 500))
                    .thenReturn(new DiagnosticoSegurancaDto(
                            -23.55, -46.63, 500, NivelRisco.BAIXO, 0.0, 0,
                            new DistribuicaoTurnoDto(0, 0, 0, 0), List.of(), List.of()
                    ));
            when(segurancaTerritorialService.listarOcorrenciasParaMapa(-23.55, -46.63, 500, 90, null, null, null))
                    .thenReturn(List.of(new PontoMapaDto(
                            -23.551, -46.633, "ASSEDIO_VERBAL", "Assédio Verbal", 2,
                            br.com.lume.api.ocorrencia.domain.FaixaHorario.NOITE, LocalDate.of(2026, 11, 1), 1
                    )));
            when(ocorrenciaService.obterOcorrenciaPorId(OCORRENCIA_ID)).thenReturn(criarOcorrenciaDetalheDto());

            mockMvc.perform(get("/v1/seguranca/diagnostico-raio")
                            .param("latitude", "-23.55")
                            .param("longitude", "-46.63")
                            .param("raioMetros", "500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nivelRisco").value("BAIXO"));

            mockMvc.perform(get("/v1/seguranca/ocorrencias-mapa")
                            .param("latitude", "-23.55")
                            .param("longitude", "-46.63"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tipoViolenciaCodigo").value("ASSEDIO_VERBAL"));

            mockMvc.perform(get("/v1/ocorrencias/{id}", OCORRENCIA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(OCORRENCIA_ID.toString()));
        }

        @Test
        @DisplayName("Deve permitir consultas públicas de estabelecimentos sem autenticação")
        void devePermitirConsultasEstabelecimentosSemToken() throws Exception {
            when(estabelecimentoService.autocompleteEstabelecimentos("Cafe", null, 10)).thenReturn(List.of());
            when(estabelecimentoService.listarEstabelecimentos(null, null, null, null, null, null, 1000, 0, 20))
                    .thenReturn(new EstabelecimentoPageDto(List.of(), 0L, 0, 0, 20));
            when(estabelecimentoService.obterEstabelecimentoPorId(ESTABELECIMENTO_ID))
                    .thenReturn(criarEstabelecimentoDetalheDto());
            when(estabelecimentoService.listarOcorrenciasEstabelecimento(ESTABELECIMENTO_ID, null, null, null, 0, 20))
                    .thenReturn(new OcorrenciaResumoPagePortDto(List.of(), 0L, 0, 0, 20));

            mockMvc.perform(get("/v1/estabelecimentos/autocomplete").param("termo", "Cafe"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/v1/estabelecimentos"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/v1/estabelecimentos/{id}", ESTABELECIMENTO_ID))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/v1/estabelecimentos/{id}/ocorrencias", ESTABELECIMENTO_ID))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Rotas Protegidas Sem Autenticação (401 Unauthorized)")
    class RotasProtegidasSemAutenticacao {

        @Test
        @DisplayName("Deve bloquear rotas protegidas sem token retornando 401 com StandardError")
        void deveRetornar401ComStandardErrorSemToken() throws Exception {
            mockMvc.perform(get("/v1/usuarios/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.path").value("/v1/usuarios/me"))
                    .andExpect(jsonPath("$.message").exists());

            mockMvc.perform(put("/v1/usuarios/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.path").value("/v1/usuarios/me"));

            mockMvc.perform(get("/v1/estabelecimentos/meu"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.path").value("/v1/estabelecimentos/meu"));

            mockMvc.perform(post("/v1/estabelecimentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(put("/v1/estabelecimentos/{id}", ESTABELECIMENTO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/v1/ocorrencias/minhas"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.path").value("/v1/ocorrencias/minhas"));

            mockMvc.perform(post("/v1/ocorrencias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/v1/ocorrencias/{id}", OCORRENCIA_ID))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(patch("/v1/ocorrencias/{id}", OCORRENCIA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/v1/ocorrencias/{id}/validacoes", OCORRENCIA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/v1/ocorrencias/{id}/validacoes", OCORRENCIA_ID))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(usuarioService, estabelecimentoService, ocorrenciaService);
        }
    }

    @Nested
    @DisplayName("Validação Real de Token JWT (JwtDecoder)")
    class ValidacaoJwtReal {

        @Test
        @DisplayName("Deve autenticar usuário com access token HS256 válido em rotas protegidas")
        void devePermitirAcessoComAccessTokenValido() throws Exception {
            when(usuarioService.obterPerfilLogado()).thenReturn(new UsuarioDetalheDto(
                    USUARIO_ID, "Maria da Silva", "52998224725", "11987654321", "usuario@lume.com.br",
                    Genero.MULHER_CIS, RacaEtnia.PARDA, OrientacaoSexual.HETEROSSEXUAL,
                    List.of("ROLE_USUARIO"), true, AGORA
            ));
            when(estabelecimentoService.obterMeuEstabelecimento()).thenReturn(Optional.empty());
            when(ocorrenciaService.listarMinhasOcorrencias(0, 20))
                    .thenReturn(new MinhaOcorrenciaPageDto(List.of(), 0L, 0, 0, 20));
            when(ocorrenciaService.registrarOcorrencia(any())).thenReturn(criarOcorrenciaDetalheDto());
            doNothing().when(ocorrenciaService).validarOcorrencia(eq(OCORRENCIA_ID), any());
            doNothing().when(ocorrenciaService).cancelarValidacaoOcorrencia(OCORRENCIA_ID);
            doNothing().when(ocorrenciaService).excluirOcorrencia(OCORRENCIA_ID);

            String bearerToken = gerarBearerToken("ROLE_USUARIO");

            mockMvc.perform(get("/v1/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("usuario@lume.com.br"));

            mockMvc.perform(get("/v1/estabelecimentos/meu").header(HttpHeaders.AUTHORIZATION, bearerToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/ocorrencias/minhas").header(HttpHeaders.AUTHORIZATION, bearerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/v1/ocorrencias")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criarRegistroOcorrenciaValido())))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/v1/ocorrencias/" + OCORRENCIA_ID));

            mockMvc.perform(post("/v1/ocorrencias/{id}/validacoes", OCORRENCIA_ID)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ValidacaoOcorrenciaRequest(TipoValidacao.PRESENCIEI))))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete("/v1/ocorrencias/{id}/validacoes", OCORRENCIA_ID)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/v1/ocorrencias/{id}", OCORRENCIA_ID)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve rejeitar refresh token usado como Bearer access token com 401")
        void deveRejeitarRefreshTokenNoHeaderAuthorization() throws Exception {
            String refreshToken = jwtService.gerarRefreshToken(USUARIO_ID, "usuario@lume.com.br");

            mockMvc.perform(get("/v1/usuarios/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));

            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Deve rejeitar token expirado, adulterado ou com issuer incorreto com 401")
        void deveRejeitarTokensInvalidosCom401() throws Exception {
            Clock clockExpirado = Clock.fixed(FIXED_INSTANT.minusSeconds(3600), ZoneOffset.UTC);
            JwtService jwtServiceExpirado = new JwtService(jwtProperties, clockExpirado);
            String tokenExpirado = jwtServiceExpirado.gerarAccessToken(USUARIO_ID, "u@lume.com.br", List.of("ROLE_USUARIO"));

            JwtProperties propsOutroSecret = new JwtProperties(
                    "outra-chave-secreta-completamente-diferente-256-bits",
                    jwtProperties.expiration(),
                    jwtProperties.refreshExpiration(),
                    jwtProperties.issuer()
            );
            String tokenAssinaturaInvalida = new JwtService(propsOutroSecret, FIXED_CLOCK)
                    .gerarAccessToken(USUARIO_ID, "u@lume.com.br", List.of("ROLE_USUARIO"));

            JwtProperties propsOutroIssuer = new JwtProperties(
                    jwtProperties.secret(),
                    jwtProperties.expiration(),
                    jwtProperties.refreshExpiration(),
                    "issuer-invalido"
            );
            String tokenIssuerInvalido = new JwtService(propsOutroIssuer, FIXED_CLOCK)
                    .gerarAccessToken(USUARIO_ID, "u@lume.com.br", List.of("ROLE_USUARIO"));

            for (String tokenInvalido : List.of(tokenExpirado, tokenAssinaturaInvalida, tokenIssuerInvalido, "token.malformado")) {
                mockMvc.perform(get("/v1/usuarios/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenInvalido))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.status").value(401));
            }

            verifyNoInteractions(usuarioService);
        }
    }

    @Nested
    @DisplayName("Autorização por Roles, @PreAuthorize e 403 Forbidden")
    class AutorizacaoPorRoles {

        @Test
        @DisplayName("Deve retornar 403 Forbidden para ROLE_USUARIO ao moderar ocorrência via PATCH /v1/ocorrencias/{id}")
        void deveRetornar403ParaUsuarioComumEmRotaModeracao() throws Exception {
            AtualizacaoStatusOcorrenciaRequest request =
                    new AtualizacaoStatusOcorrenciaRequest(AtualizacaoStatusOcorrenciaRequest.StatusEnum.SOB_ANALISE);

            mockMvc.perform(patch("/v1/ocorrencias/{id}", OCORRENCIA_ID)
                            .header(HttpHeaders.AUTHORIZATION, gerarBearerToken("ROLE_USUARIO"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.path").value("/v1/ocorrencias/" + OCORRENCIA_ID));

            verifyNoInteractions(ocorrenciaService);
        }

        @Test
        @DisplayName("Deve permitir PATCH /v1/ocorrencias/{id} para ROLE_MODERADOR, ROLE_ADMIN e role sem prefixo ROLE_")
        void devePermitirModeradorEAdminNaModeracao() throws Exception {
            AtualizacaoStatusOcorrenciaRequest request =
                    new AtualizacaoStatusOcorrenciaRequest(AtualizacaoStatusOcorrenciaRequest.StatusEnum.REMOVIDA);
            when(ocorrenciaService.atualizarStatusOcorrencia(eq(OCORRENCIA_ID), any()))
                    .thenReturn(criarOcorrenciaDetalheDto());

            for (String role : List.of("ROLE_MODERADOR", "ROLE_ADMIN", "MODERADOR", "ADMIN")) {
                mockMvc.perform(patch("/v1/ocorrencias/{id}", OCORRENCIA_ID)
                                .header(HttpHeaders.AUTHORIZATION, gerarBearerToken(role))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(OCORRENCIA_ID.toString()));
            }
        }
    }

    @Nested
    @DisplayName("Filtros HTTP, CORS e Bean Validation no DispatcherServlet")
    class FiltrosCorsEValidacaoWebMvc {

        @Test
        @DisplayName("Deve propagar X-Request-Id válido e gerar UUID quando ausente ou inválido")
        void deveGerenciarHeaderRequestId() throws Exception {
            when(catalogoService.listarTiposViolenciaAtivos()).thenReturn(List.of());

            mockMvc.perform(get("/v1/catalogos/tipos-violencia")
                            .header(RequestIdFilter.HEADER_NAME, "req-flutter-12345"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req-flutter-12345"));

            mockMvc.perform(get("/v1/catalogos/tipos-violencia"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            RequestIdFilter.HEADER_NAME,
                            Matchers.matchesRegex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                    ));

            mockMvc.perform(get("/v1/catalogos/tipos-violencia")
                            .header(RequestIdFilter.HEADER_NAME, "header invalido!!!"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            RequestIdFilter.HEADER_NAME,
                            Matchers.matchesRegex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                    ));
        }

        @Test
        @DisplayName("Deve aceitar preflight CORS de origem configurada e rejeitar origem não permitida")
        void deveValidarCorsPreflight() throws Exception {
            mockMvc.perform(options("/v1/ocorrencias")
                            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Matchers.containsString("Location")));

            mockMvc.perform(options("/v1/ocorrencias")
                            .header(HttpHeaders.ORIGIN, "https://origem-nao-autorizada.com")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("Deve validar payload autenticado via GlobalExceptionHandler retornando 400 com fields")
        void deveRetornar400QuandoPayloadAutenticadoInvalido() throws Exception {
            CriacaoEstabelecimentoRequest requestInvalido = new CriacaoEstabelecimentoRequest();

            mockMvc.perform(post("/v1/estabelecimentos")
                            .header(HttpHeaders.AUTHORIZATION, gerarBearerToken("ROLE_USUARIO"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fields").isArray())
                    .andExpect(jsonPath("$.fields", Matchers.not(Matchers.empty())));

            AtualizacaoUsuarioRequest atualizacaoInvalida = new AtualizacaoUsuarioRequest().nomeCompleto("A");
            mockMvc.perform(put("/v1/usuarios/me")
                            .header(HttpHeaders.AUTHORIZATION, gerarBearerToken("ROLE_USUARIO"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(atualizacaoInvalida)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fields[0].field").value("nomeCompleto"));

            verifyNoInteractions(estabelecimentoService, usuarioService);
        }
    }
}
