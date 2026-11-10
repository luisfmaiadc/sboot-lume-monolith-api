package br.com.lume.api.ocorrencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.domain.StatusOcorrencia;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.repository.OcorrenciaRepository;
import br.com.lume.api.shared.port.EstatisticasOcorrenciaEstabelecimentoDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcorrenciaEstabelecimentoQueryAdapter - Testes Unitários")
class OcorrenciaEstabelecimentoQueryAdapterTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 0, 0, 0, ZoneOffset.UTC);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;

    @Mock
    private CatalogoInternalFacade catalogoInternalFacade;

    @Mock
    private CalculadoraReputacaoRiscoService calculadoraReputacaoRiscoService;

    private OcorrenciaEstabelecimentoQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        OcorrenciaMapper ocorrenciaMapper = Mappers.getMapper(OcorrenciaMapper.class);
        Clock clock = Clock.fixed(AGORA.toInstant(), ZoneOffset.UTC);
        adapter = new OcorrenciaEstabelecimentoQueryAdapter(
                ocorrenciaRepository,
                catalogoInternalFacade,
                calculadoraReputacaoRiscoService,
                ocorrenciaMapper,
                clock,
                ZONA_NEGOCIO
        );
    }

    private Ocorrencia criarOcorrencia(FaixaHorario faixaHorario) {
        return Ocorrencia.registrar(
                ESTABELECIMENTO_ID,
                -23.56,
                -46.65,
                "Rua Augusta",
                "100",
                "Consolação",
                "São Paulo",
                "SP",
                LocalDate.of(2026, 5, 9),
                faixaHorario,
                1,
                2,
                TipoAmbiente.ESTABELECIMENTO,
                AUTOR_ID,
                AGORA
        );
    }

    @Nested
    @DisplayName("listarPorEstabelecimento")
    class ListarPorEstabelecimento {

        @Test
        @DisplayName("Deve listar ocorrências resumidas paginadas enriquecidas com tipo de violência e marcador")
        void deveListarOcorrenciasResumidasComSucesso() {
            // Arrange
            Ocorrencia o1 = criarOcorrencia(FaixaHorario.NOITE);
            Page<Ocorrencia> pagina = new PageImpl<>(List.of(o1), PageRequest.of(0, 10), 1);

            when(ocorrenciaRepository.listarPublicadasPorEstabelecimento(
                    ESTABELECIMENTO_ID, 1, 2, FaixaHorario.NOITE, PageRequest.of(0, 10)
            )).thenReturn(pagina);
            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(new TipoViolenciaDto(1, "ASSEDIO", "Assédio Verbal", 3, 1, true)));
            when(catalogoInternalFacade.listarMarcadoresPorIds(anyCollection()))
                    .thenReturn(List.of(new MarcadorVulnerabilidadeDto(2, "ESCURO", "Pouca Iluminação", null, true)));

            // Act
            OcorrenciaResumoPagePortDto result = adapter.listarPorEstabelecimento(
                    ESTABELECIMENTO_ID, 1, 2, "NOITE", 0, 10
            );

            // Assert
            assertThat(result.totalElements()).isEqualTo(1L);
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(o1.getId());
            assertThat(result.content().getFirst().tipoViolenciaNome()).isEqualTo("Assédio Verbal");
            assertThat(result.content().getFirst().marcadorNome()).isEqualTo("Pouca Iluminação");
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver ocorrências publicadas")
        void deveRetornarPaginaVaziaQuandoSemOcorrencias() {
            // Arrange
            Page<Ocorrencia> paginaVazia = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(ocorrenciaRepository.listarPublicadasPorEstabelecimento(
                    ESTABELECIMENTO_ID, null, null, null, PageRequest.of(0, 10)
            )).thenReturn(paginaVazia);

            // Act
            OcorrenciaResumoPagePortDto result = adapter.listarPorEstabelecimento(
                    ESTABELECIMENTO_ID, null, null, null, 0, 10
            );

            // Assert
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("obterEstatisticasPorEstabelecimento")
    class ObterEstatisticasPorEstabelecimento {

        @Test
        @DisplayName("Deve contabilizar total de ocorrências e distribuição por turno do estabelecimento")
        void deveContabilizarEstatisticasPorEstabelecimento() {
            // Arrange
            List<Ocorrencia> ocorrencias = List.of(
                    criarOcorrencia(FaixaHorario.MADRUGADA),
                    criarOcorrencia(FaixaHorario.MANHA),
                    criarOcorrencia(FaixaHorario.TARDE),
                    criarOcorrencia(FaixaHorario.NOITE),
                    criarOcorrencia(FaixaHorario.NOITE)
            );
            when(ocorrenciaRepository.findByEstabelecimentoIdAndStatusOrderByDataOcorrenciaDescCriadoEmDesc(
                    ESTABELECIMENTO_ID, StatusOcorrencia.PUBLICADA
            )).thenReturn(ocorrencias);

            // Act
            EstatisticasOcorrenciaEstabelecimentoDto stats =
                    adapter.obterEstatisticasPorEstabelecimento(ESTABELECIMENTO_ID);

            // Assert
            assertThat(stats.totalOcorrenciasHistoricas()).isEqualTo(5);
            assertThat(stats.madrugada()).isEqualTo(1);
            assertThat(stats.manha()).isEqualTo(1);
            assertThat(stats.tarde()).isEqualTo(1);
            assertThat(stats.noite()).isEqualTo(2);
            assertThat(stats.distribuicaoTipoViolencia()).containsEntry(1, 5);
        }

        @Test
        @DisplayName("Deve retornar estatísticas zeradas quando estabelecimentoId for nulo")
        void deveRetornarEstatisticasZeradasQuandoEstabelecimentoIdNulo() {
            // Arrange & Act
            EstatisticasOcorrenciaEstabelecimentoDto stats = adapter.obterEstatisticasPorEstabelecimento(null);

            // Assert
            assertThat(stats.totalOcorrenciasHistoricas()).isZero();
            assertThat(stats.madrugada()).isZero();
            assertThat(stats.manha()).isZero();
            assertThat(stats.tarde()).isZero();
            assertThat(stats.noite()).isZero();
            assertThat(stats.distribuicaoTipoViolencia()).isEmpty();
        }
    }

    @Nested
    @DisplayName("calcularScoreReputacao")
    class CalcularScoreReputacao {

        @Test
        @DisplayName("Deve retornar score padrão 5.00 quando estabelecimentoId for nulo")
        void deveRetornarScorePadraoQuandoIdNulo() {
            BigDecimal score = adapter.calcularScoreReputacao(null);
            assertThat(score).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Deve retornar score padrão 5.00 quando não houver ocorrências publicadas")
        void deveRetornarScorePadraoQuandoSemOcorrencias() {
            when(ocorrenciaRepository.findByEstabelecimentoIdAndStatusOrderByDataOcorrenciaDescCriadoEmDesc(
                    ESTABELECIMENTO_ID, StatusOcorrencia.PUBLICADA
            )).thenReturn(List.of());

            BigDecimal score = adapter.calcularScoreReputacao(ESTABELECIMENTO_ID);
            assertThat(score).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Deve calcular score de reputação somando impactos ponderados das ocorrências")
        void deveCalcularScoreReputacaoComSucesso() {
            Ocorrencia o1 = criarOcorrencia(FaixaHorario.NOITE);
            when(ocorrenciaRepository.findByEstabelecimentoIdAndStatusOrderByDataOcorrenciaDescCriadoEmDesc(
                    ESTABELECIMENTO_ID, StatusOcorrencia.PUBLICADA
            )).thenReturn(List.of(o1));

            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(new TipoViolenciaDto(1, "ASSEDIO", "Assédio Verbal", 3, 1, true)));

            when(calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(
                    eq(3), eq(o1.getDataOcorrencia()), eq(o1.getTotalValidacoes()), any(LocalDate.class)
            )).thenReturn(new BigDecimal("3.00"));

            when(calculadoraReputacaoRiscoService.calcularScoreEstabelecimento(new BigDecimal("3.00")))
                    .thenReturn(new BigDecimal("4.70"));

            BigDecimal score = adapter.calcularScoreReputacao(ESTABELECIMENTO_ID);

            assertThat(score).isEqualByComparingTo(new BigDecimal("4.70"));
        }
    }
}
