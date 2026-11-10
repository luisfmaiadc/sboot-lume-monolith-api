package br.com.lume.api.ocorrencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;
import br.com.lume.api.ocorrencia.dto.DiagnosticoSegurancaDto;
import br.com.lume.api.ocorrencia.dto.PontoMapaDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.repository.AgrupamentoDiagnosticoProjection;
import br.com.lume.api.ocorrencia.repository.OcorrenciaRepository;
import br.com.lume.api.shared.config.GeoProperties;
import br.com.lume.api.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("SegurancaTerritorialService - Testes Unitários")
class SegurancaTerritorialServiceTest {

    private static final Instant INSTANT_FIXO = Instant.parse("2026-05-10T14:00:00Z");
    private static final OffsetDateTime AGORA = OffsetDateTime.ofInstant(INSTANT_FIXO, ZoneOffset.UTC);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATA_HOJE = LocalDate.of(2026, 5, 10);
    private static final UUID AUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;

    @Mock
    private CatalogoInternalFacade catalogoInternalFacade;

    @Mock
    private CalculadoraReputacaoRiscoService calculadoraReputacaoRiscoService;

    private GeoProperties geoProperties;
    private SegurancaTerritorialService segurancaTerritorialService;

    @BeforeEach
    void setUp() {
        Clock clockFixo = Clock.fixed(INSTANT_FIXO, ZoneOffset.UTC);
        geoProperties = new GeoProperties(
                new GeoProperties.DiagnosticoGeoProperties(50, 500, 2000),
                new GeoProperties.MapaGeoProperties(50, 500, 2000, 1000, 730),
                new GeoProperties.EstabelecimentoGeoProperties(100, 1000, 5000)
        );
        OcorrenciaMapper ocorrenciaMapper = Mappers.getMapper(OcorrenciaMapper.class);
        segurancaTerritorialService = new SegurancaTerritorialService(
                ocorrenciaRepository,
                catalogoInternalFacade,
                calculadoraReputacaoRiscoService,
                geoProperties,
                clockFixo,
                ZONA_NEGOCIO,
                ocorrenciaMapper
        );
    }

    private Ocorrencia criarOcorrencia(FaixaHorario faixaHorario, int tipoViolenciaId, Integer marcadorId) {
        return Ocorrencia.registrar(
                null,
                -23.56123,
                -46.65123,
                "Rua Augusta",
                "100",
                "Consolação",
                "São Paulo",
                "SP",
                DATA_HOJE.minusDays(5),
                faixaHorario,
                tipoViolenciaId,
                marcadorId,
                TipoAmbiente.VIA_PUBLICA,
                AUTOR_ID,
                AGORA
        );
    }

    private AgrupamentoDiagnosticoProjection criarProjection(
            Integer tipoViolenciaId,
            Integer marcadorId,
            String faixaHorario,
            LocalDate dataOcorrencia,
            int validacoesComputadas,
            int quantidade
    ) {
        return new AgrupamentoDiagnosticoProjection() {
            @Override
            public Integer getTipoViolenciaId() {
                return tipoViolenciaId;
            }

            @Override
            public Integer getMarcadorVulnerabilidadeId() {
                return marcadorId;
            }

            @Override
            public String getFaixaHorario() {
                return faixaHorario;
            }

            @Override
            public LocalDate getDataOcorrencia() {
                return dataOcorrencia;
            }

            @Override
            public int getValidacoesComputadas() {
                return validacoesComputadas;
            }

            @Override
            public int getQuantidade() {
                return quantidade;
            }
        };
    }

    @Nested
    @DisplayName("obterDiagnosticoRaio")
    class ObterDiagnosticoRaio {

        @Test
        @DisplayName("Deve calcular diagnóstico de segurança no raio padrão de 500m aplicando limiar k >= 3 nas distribuições e discretizando scoreRisco em 0.50")
        void deveCalcularDiagnosticoNoRaioPadraoComSucesso() {
            // Arrange
            AgrupamentoDiagnosticoProjection g1 = criarProjection(1, 10, "NOITE", DATA_HOJE.minusDays(5), 0, 3);
            AgrupamentoDiagnosticoProjection g2 = criarProjection(1, null, "MADRUGADA", DATA_HOJE.minusDays(5), 0, 1);
            AgrupamentoDiagnosticoProjection g3 = criarProjection(2, 10, "MANHA", DATA_HOJE.minusDays(5), 0, 1);

            TipoViolenciaDto tv1 = new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);
            TipoViolenciaDto tv2 = new TipoViolenciaDto(2, "IMPORTUNACAO", "Importunação", 5, 2, true);
            MarcadorVulnerabilidadeDto m10 = new MarcadorVulnerabilidadeDto(10, "ESCURO", "Pouca Iluminação", null, true);

            when(ocorrenciaRepository.agruparPublicadasNoRaio(-23.56, -46.65, 500, DATA_HOJE.minusDays(365)))
                    .thenReturn(List.of(g1, g2, g3));
            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(tv1, tv2));
            when(catalogoInternalFacade.listarMarcadoresPorIds(anyCollection()))
                    .thenReturn(List.of(m10));
            when(calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(3, DATA_HOJE.minusDays(5), 0, DATA_HOJE))
                    .thenReturn(new BigDecimal("3.00"));
            when(calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(5, DATA_HOJE.minusDays(5), 0, DATA_HOJE))
                    .thenReturn(new BigDecimal("5.00"));
            when(calculadoraReputacaoRiscoService.calcularScoreRiscoViaPublica(new BigDecimal("17.00")))
                    .thenReturn(new BigDecimal("2.80"));
            when(calculadoraReputacaoRiscoService.classificarNivelRisco(new BigDecimal("3.00")))
                    .thenReturn(NivelRisco.MEDIO);

            // Act
            DiagnosticoSegurancaDto response = segurancaTerritorialService.obterDiagnosticoRaio(-23.56, -46.65, null);

            // Assert
            assertThat(response.raioMetros()).isEqualTo(500);
            assertThat(response.totalOcorrencias()).isEqualTo(5);
            // 2.80 discretizado em degraus de 0.50 (HALF_UP) -> 3.00
            assertThat(response.scoreRisco()).isEqualTo(3.00);
            assertThat(response.nivelRisco()).isEqualTo(NivelRisco.MEDIO);
            // Turnos com < 3 ocorrências são suprimidos para 0; NOITE tem 3 (>= 3, mantido)
            assertThat(response.distribuicaoTurno().madrugada()).isZero();
            assertThat(response.distribuicaoTurno().manha()).isZero();
            assertThat(response.distribuicaoTurno().tarde()).isZero();
            assertThat(response.distribuicaoTurno().noite()).isEqualTo(3);
            // Violência 1 tem total 4 (>= 3, mantida); Violência 2 tem total 1 (< 3, suprimida)
            assertThat(response.distribuicaoViolencia()).hasSize(1);
            assertThat(response.distribuicaoViolencia().getFirst().codigo()).isEqualTo("ASSEDIO_VERBAL");
            assertThat(response.distribuicaoViolencia().getFirst().total()).isEqualTo(4);
            // Marcador 10 tem total 4 (3 + 1 >= 3, mantido)
            assertThat(response.distribuicaoMarcadores()).hasSize(1);
            assertThat(response.distribuicaoMarcadores().getFirst().codigo()).isEqualTo("ESCURO");
            assertThat(response.distribuicaoMarcadores().getFirst().total()).isEqualTo(4);
        }

        @Test
        @DisplayName("Deve agrupar contagens menores que 3 sob categoria OUTROS quando soma acumulada for >= 3")
        void deveAgruparEmOutrosQuandoSomaResidualAtingirLimiar() {
            // Arrange
            AgrupamentoDiagnosticoProjection g1 = criarProjection(1, 10, "NOITE", DATA_HOJE.minusDays(5), 0, 2);
            AgrupamentoDiagnosticoProjection g2 = criarProjection(2, 20, "MANHA", DATA_HOJE.minusDays(5), 0, 2);

            TipoViolenciaDto tv1 = new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);
            TipoViolenciaDto tv2 = new TipoViolenciaDto(2, "IMPORTUNACAO", "Importunação", 5, 2, true);
            MarcadorVulnerabilidadeDto m10 = new MarcadorVulnerabilidadeDto(10, "ESCURO", "Pouca Iluminação", null, true);
            MarcadorVulnerabilidadeDto m20 = new MarcadorVulnerabilidadeDto(20, "ISOLADO", "Local Isolado", null, true);

            when(ocorrenciaRepository.agruparPublicadasNoRaio(-23.56, -46.65, 500, DATA_HOJE.minusDays(365)))
                    .thenReturn(List.of(g1, g2));
            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(tv1, tv2));
            when(catalogoInternalFacade.listarMarcadoresPorIds(anyCollection()))
                    .thenReturn(List.of(m10, m20));
            when(calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(3, DATA_HOJE.minusDays(5), 0, DATA_HOJE))
                    .thenReturn(new BigDecimal("3.00"));
            when(calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(5, DATA_HOJE.minusDays(5), 0, DATA_HOJE))
                    .thenReturn(new BigDecimal("5.00"));
            when(calculadoraReputacaoRiscoService.calcularScoreRiscoViaPublica(new BigDecimal("16.00")))
                    .thenReturn(new BigDecimal("3.20"));
            when(calculadoraReputacaoRiscoService.classificarNivelRisco(new BigDecimal("3.00")))
                    .thenReturn(NivelRisco.MEDIO);

            // Act
            DiagnosticoSegurancaDto response = segurancaTerritorialService.obterDiagnosticoRaio(-23.56, -46.65, null);

            // Assert
            assertThat(response.scoreRisco()).isEqualTo(3.00);
            assertThat(response.distribuicaoTurno().noite()).isZero();
            assertThat(response.distribuicaoTurno().manha()).isZero();
            assertThat(response.distribuicaoViolencia()).hasSize(1);
            assertThat(response.distribuicaoViolencia().getFirst().codigo()).isEqualTo("OUTROS");
            assertThat(response.distribuicaoViolencia().getFirst().total()).isEqualTo(4);

            assertThat(response.distribuicaoMarcadores()).hasSize(1);
            assertThat(response.distribuicaoMarcadores().getFirst().codigo()).isEqualTo("OUTROS");
            assertThat(response.distribuicaoMarcadores().getFirst().total()).isEqualTo(4);
        }

        @Test
        @DisplayName("Deve zerar todos os turnos quando totalOcorrencias for menor que 3 preservando totalOcorrencias exato")
        void deveZerarTurnosQuandoTotalOcorrenciasMenorQueTres() {
            // Arrange
            AgrupamentoDiagnosticoProjection g1 = criarProjection(1, 10, "NOITE", DATA_HOJE.minusDays(2), 0, 2);
            TipoViolenciaDto tv1 = new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);
            MarcadorVulnerabilidadeDto m10 = new MarcadorVulnerabilidadeDto(10, "ESCURO", "Pouca Iluminação", null, true);

            when(ocorrenciaRepository.agruparPublicadasNoRaio(-23.56, -46.65, 500, DATA_HOJE.minusDays(365)))
                    .thenReturn(List.of(g1));
            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(tv1));
            when(catalogoInternalFacade.listarMarcadoresPorIds(anyCollection()))
                    .thenReturn(List.of(m10));
            when(calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(3, DATA_HOJE.minusDays(2), 0, DATA_HOJE))
                    .thenReturn(new BigDecimal("3.00"));
            when(calculadoraReputacaoRiscoService.calcularScoreRiscoViaPublica(new BigDecimal("6.00")))
                    .thenReturn(new BigDecimal("1.20"));
            when(calculadoraReputacaoRiscoService.classificarNivelRisco(new BigDecimal("1.00")))
                    .thenReturn(NivelRisco.BAIXO);

            // Act
            DiagnosticoSegurancaDto response = segurancaTerritorialService.obterDiagnosticoRaio(-23.56, -46.65, null);

            // Assert
            assertThat(response.totalOcorrencias()).isEqualTo(2);
            assertThat(response.scoreRisco()).isEqualTo(1.00);
            assertThat(response.nivelRisco()).isEqualTo(NivelRisco.BAIXO);
            assertThat(response.distribuicaoTurno().madrugada()).isZero();
            assertThat(response.distribuicaoTurno().manha()).isZero();
            assertThat(response.distribuicaoTurno().tarde()).isZero();
            assertThat(response.distribuicaoTurno().noite()).isZero();
            assertThat(response.distribuicaoViolencia()).isEmpty();
            assertThat(response.distribuicaoMarcadores()).isEmpty();
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando coordenadas forem nulas ou raioMetros estiver fora do limite configurado")
        void deveLancarBusinessRuleExceptionQuandoParametrosInvalidos() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> segurancaTerritorialService.obterDiagnosticoRaio(null, -46.65, 500))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Coordenadas de latitude e longitude são obrigatórias.");

            assertThatThrownBy(() -> segurancaTerritorialService.obterDiagnosticoRaio(-23.56, -46.65, 49))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O raio espacial de diagnóstico deve estar entre 50 e 2000 metros.");

            assertThatThrownBy(() -> segurancaTerritorialService.obterDiagnosticoRaio(-23.56, -46.65, 2001))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O raio espacial de diagnóstico deve estar entre 50 e 2000 metros.");
        }
    }

    @Nested
    @DisplayName("listarOcorrenciasParaMapa")
    class ListarOcorrenciasParaMapa {

        @Test
        @DisplayName("Deve listar pontos para o mapa usando janela padrão de 90 dias e atenuar data e coordenadas em via pública")
        void deveListarPontosParaMapaComSucesso() {
            // Arrange
            Ocorrencia o1 = criarOcorrencia(FaixaHorario.NOITE, 1, 10);
            TipoViolenciaDto tv1 = new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);

            when(ocorrenciaRepository.listarPublicadasParaMapa(
                    -23.56, -46.65, 500.0, DATA_HOJE.minusDays(90), 1, 10, "NOITE", 1000
            )).thenReturn(List.of(o1));
            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(tv1));

            // Act
            List<PontoMapaDto> resultado = segurancaTerritorialService.listarOcorrenciasParaMapa(
                    -23.56, -46.65, null, null, 1, 10, FaixaHorario.NOITE
            );

            // Assert
            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().latitude()).isEqualTo(-23.561);
            assertThat(resultado.getFirst().longitude()).isEqualTo(-46.651);
            assertThat(resultado.getFirst().dataOcorrencia()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(resultado.getFirst().tipoViolenciaCodigo()).isEqualTo("ASSEDIO_VERBAL");
            assertThat(resultado.getFirst().pesoGravidade()).isEqualTo(3);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando dias estiver fora do intervalo 1..730")
        void deveLancarBusinessRuleExceptionQuandoDiasInvalido() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> segurancaTerritorialService.listarOcorrenciasParaMapa(
                    -23.56, -46.65, 500, 0, null, null, null
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A janela retroativa de dias deve estar entre 1 e 730.");

            assertThatThrownBy(() -> segurancaTerritorialService.listarOcorrenciasParaMapa(
                    -23.56, -46.65, 500, 731, null, null, null
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A janela retroativa de dias deve estar entre 1 e 730.");
        }
    }
}
