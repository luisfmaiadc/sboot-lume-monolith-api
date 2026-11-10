package br.com.lume.api.ocorrencia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.lume.api.model.DiagnosticoSegurancaResponse;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.PontoMapaOcorrenciaResponse;
import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.ocorrencia.dto.DiagnosticoSegurancaDto;
import br.com.lume.api.ocorrencia.dto.PontoMapaDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.service.SegurancaTerritorialService;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("SegurancaController - Testes Unitários")
class SegurancaControllerTest {

    @Mock
    private SegurancaTerritorialService segurancaTerritorialService;

    private OcorrenciaMapper ocorrenciaMapper;
    private SegurancaController segurancaController;

    @BeforeEach
    void setUp() {
        ocorrenciaMapper = Mappers.getMapper(OcorrenciaMapper.class);
        segurancaController = new SegurancaController(segurancaTerritorialService, ocorrenciaMapper);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com diagnóstico de segurança no raio solicitado")
    void deveRetornar200OkComDiagnosticoRaio() {
        // Arrange
        DiagnosticoSegurancaDto diagnostico = new DiagnosticoSegurancaDto(
                -23.56,
                -46.65,
                500,
                NivelRisco.BAIXO,
                1.20,
                2,
                new DistribuicaoTurnoDto(0, 0, 1, 1),
                List.of(),
                List.of()
        );
        when(segurancaTerritorialService.obterDiagnosticoRaio(-23.56, -46.65, 500))
                .thenReturn(diagnostico);

        // Act
        ResponseEntity<DiagnosticoSegurancaResponse> response =
                segurancaController.obterDiagnosticoRaio(-23.56, -46.65, 500);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRaioMetros()).isEqualTo(500);
        assertThat(response.getBody().getScoreRisco()).isEqualTo(1.20);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com lista de ocorrências para o mapa")
    void deveRetornar200OkComListaDeOcorrenciasParaMapa() {
        // Arrange
        PontoMapaDto ponto = new PontoMapaDto(
                -23.56,
                -46.65,
                "ASSEDIO_VERBAL",
                "Assédio Verbal",
                3,
                br.com.lume.api.ocorrencia.domain.FaixaHorario.NOITE,
                LocalDate.of(2026, 5, 1),
                1
        );
        when(segurancaTerritorialService.listarOcorrenciasParaMapa(
                -23.56, -46.65, 500, 90, 1, 2, br.com.lume.api.ocorrencia.domain.FaixaHorario.NOITE
        )).thenReturn(List.of(ponto));

        // Act
        ResponseEntity<List<PontoMapaOcorrenciaResponse>> response =
                segurancaController.listarOcorrenciasParaMapa(-23.56, -46.65, 500, 90, 1, 2, FaixaHorario.NOITE);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getLatitude()).isEqualTo(-23.56);
        assertThat(response.getBody().getFirst().getDataOcorrencia()).isEqualTo(LocalDate.of(2026, 5, 1));
    }
}
