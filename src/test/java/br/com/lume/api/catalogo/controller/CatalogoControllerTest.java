package br.com.lume.api.catalogo.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.catalogo.mapper.CatalogoMapper;
import br.com.lume.api.catalogo.service.CatalogoService;
import br.com.lume.api.shared.exception.GlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoController - Testes Unitários (Standalone MockMvc)")
class CatalogoControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private CatalogoService catalogoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CatalogoMapper catalogoMapper = Mappers.getMapper(CatalogoMapper.class);
        CatalogoController controller = new CatalogoController(catalogoService, catalogoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(FIXED_CLOCK))
                .build();
    }

    @Nested
    @DisplayName("GET /v1/catalogos/tipos-violencia")
    class ListarTiposViolencia {

        @Test
        @DisplayName("Deve retornar 200 OK com lista de tipos de violência ativos")
        void deveRetornar200ComListaDeTiposViolencia() throws Exception {
            TipoViolenciaDto item = new TipoViolenciaDto(
                    1, "ASSEDIO_VERBAL", "Assédio Verbal / Importunação", 2, 1, true
            );
            when(catalogoService.listarTiposViolenciaAtivos()).thenReturn(List.of(item));

            mockMvc.perform(get("/v1/catalogos/tipos-violencia")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].codigo").value("ASSEDIO_VERBAL"))
                    .andExpect(jsonPath("$[0].nome").value("Assédio Verbal / Importunação"))
                    .andExpect(jsonPath("$[0].pesoGravidade").value(2))
                    .andExpect(jsonPath("$[0].ordemExibicao").value(1));

            verify(catalogoService).listarTiposViolenciaAtivos();
        }
    }

    @Nested
    @DisplayName("GET /v1/catalogos/marcadores-vulnerabilidade")
    class ListarMarcadoresVulnerabilidade {

        @Test
        @DisplayName("Deve retornar 200 OK com lista de marcadores de vulnerabilidade ativos")
        void deveRetornar200ComListaDeMarcadores() throws Exception {
            MarcadorVulnerabilidadeDto item = new MarcadorVulnerabilidadeDto(
                    1, "MISOGINIA", "Violência contra a Mulher / Misoginia",
                    "Ataque motivado pelo fato de a vítima ser mulher.", true
            );
            when(catalogoService.listarMarcadoresVulnerabilidadeAtivos()).thenReturn(List.of(item));

            mockMvc.perform(get("/v1/catalogos/marcadores-vulnerabilidade")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].codigo").value("MISOGINIA"))
                    .andExpect(jsonPath("$[0].nome").value("Violência contra a Mulher / Misoginia"))
                    .andExpect(jsonPath("$[0].descricao").value("Ataque motivado pelo fato de a vítima ser mulher."));

            verify(catalogoService).listarMarcadoresVulnerabilidadeAtivos();
        }
    }

    @Nested
    @DisplayName("GET /v1/catalogos/categorias-estabelecimentos")
    class ListarCategoriasEstabelecimentos {

        @Test
        @DisplayName("Deve retornar 200 OK com lista de categorias de estabelecimentos ativas")
        void deveRetornar200ComListaDeCategorias() throws Exception {
            CategoriaEstabelecimentoDto item = new CategoriaEstabelecimentoDto(
                    1, "BAR_RESTAURANTE", "Bares e Restaurantes", true
            );
            when(catalogoService.listarCategoriasEstabelecimentosAtivas()).thenReturn(List.of(item));

            mockMvc.perform(get("/v1/catalogos/categorias-estabelecimentos")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].codigo").value("BAR_RESTAURANTE"))
                    .andExpect(jsonPath("$[0].nome").value("Bares e Restaurantes"));

            verify(catalogoService).listarCategoriasEstabelecimentosAtivas();
        }
    }
}
