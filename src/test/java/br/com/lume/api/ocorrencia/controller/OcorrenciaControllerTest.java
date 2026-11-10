package br.com.lume.api.ocorrencia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.model.AtualizacaoStatusOcorrenciaRequest;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.MinhaOcorrenciaPageResult;
import br.com.lume.api.model.OcorrenciaDetalheResponse;
import br.com.lume.api.model.RegistroOcorrenciaRequest;
import br.com.lume.api.model.TipoAmbiente;
import br.com.lume.api.model.TipoValidacao;
import br.com.lume.api.model.ValidacaoOcorrenciaRequest;
import br.com.lume.api.ocorrencia.command.AtualizarStatusOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.RegistrarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.ValidarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaPageDto;
import br.com.lume.api.ocorrencia.dto.OcorrenciaDetalheDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.service.OcorrenciaService;
import java.time.LocalDate;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcorrenciaController - Testes Unitários")
class OcorrenciaControllerTest {

    private static final UUID OCORRENCIA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate DATA_REGISTRO = LocalDate.of(2026, 5, 10);

    @Mock
    private OcorrenciaService ocorrenciaService;

    private OcorrenciaMapper ocorrenciaMapper;
    private OcorrenciaController ocorrenciaController;

    @BeforeEach
    void setUp() {
        ocorrenciaMapper = Mappers.getMapper(OcorrenciaMapper.class);
        ocorrenciaController = new OcorrenciaController(ocorrenciaService, ocorrenciaMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private OcorrenciaDetalheDto criarDetalheDto() {
        TipoViolenciaDto tv = new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);
        return new OcorrenciaDetalheDto(
                OCORRENCIA_ID,
                null,
                null,
                -23.56,
                -46.65,
                "Rua Augusta",
                null,
                "Consolação",
                "São Paulo",
                "SP",
                LocalDate.of(2026, 5, 9),
                br.com.lume.api.ocorrencia.domain.FaixaHorario.NOITE,
                tv,
                null,
                br.com.lume.api.ocorrencia.domain.TipoAmbiente.VIA_PUBLICA,
                0,
                DATA_REGISTRO
        );
    }

    @Nested
    @DisplayName("Endpoints de Ocorrência")
    class EndpointsOcorrencia {

        @Test
        @DisplayName("Deve retornar 201 Created com header Location ao registrar ocorrência")
        void deveRetornar201CreatedAoRegistrarOcorrencia() {
            // Arrange
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    LocalDate.of(2026, 5, 9), FaixaHorario.NOITE, 1
            );
            request.setLatitude(-23.56);
            request.setLongitude(-46.65);
            request.setLogradouro("Rua Augusta");
            request.setBairro("Consolação");
            request.setCidade("São Paulo");
            request.setEstado("SP");
            request.setTipoAmbiente(TipoAmbiente.VIA_PUBLICA);
            OcorrenciaDetalheDto detalhe = criarDetalheDto();
            when(ocorrenciaService.registrarOcorrencia(any(RegistrarOcorrenciaCommand.class))).thenReturn(detalhe);

            // Act
            ResponseEntity<OcorrenciaDetalheResponse> response = ocorrenciaController.registrarOcorrencia(request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getHeaders().getLocation())
                    .hasToString("http://localhost/v1/ocorrencias/" + OCORRENCIA_ID);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(OCORRENCIA_ID);
        }

        @Test
        @DisplayName("Deve retornar 200 OK ao obter ocorrência por id")
        void deveRetornar200OkAoObterOcorrenciaPorId() {
            // Arrange
            OcorrenciaDetalheDto detalhe = criarDetalheDto();
            when(ocorrenciaService.obterOcorrenciaPorId(OCORRENCIA_ID)).thenReturn(detalhe);

            // Act
            ResponseEntity<OcorrenciaDetalheResponse> response = ocorrenciaController.obterOcorrenciaPorId(OCORRENCIA_ID);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(OCORRENCIA_ID);
        }

        @Test
        @DisplayName("Deve retornar 200 OK ao listar minhas ocorrências")
        void deveRetornar200OkAoListarMinhasOcorrencias() {
            // Arrange
            MinhaOcorrenciaPageDto pageDto = new MinhaOcorrenciaPageDto(List.of(), 0L, 0, 0, 20);
            when(ocorrenciaService.listarMinhasOcorrencias(0, 20)).thenReturn(pageDto);

            // Act
            ResponseEntity<MinhaOcorrenciaPageResult> response = ocorrenciaController.listarMinhasOcorrencias(0, 20);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Deve retornar 204 No Content ao excluir ocorrência")
        void deveRetornar204NoContentAoExcluirOcorrencia() {
            // Arrange & Act
            ResponseEntity<Void> response = ocorrenciaController.excluirOcorrencia(OCORRENCIA_ID);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(ocorrenciaService).excluirOcorrencia(OCORRENCIA_ID);
        }

        @Test
        @DisplayName("Deve retornar 200 OK ao moderar status da ocorrência (PATCH /v1/ocorrencias/{id})")
        void deveRetornar200AoModerarStatusOcorrencia() {
            // Arrange
            AtualizacaoStatusOcorrenciaRequest request = new AtualizacaoStatusOcorrenciaRequest(
                    AtualizacaoStatusOcorrenciaRequest.StatusEnum.SOB_ANALISE
            );
            OcorrenciaDetalheDto detalhe = criarDetalheDto();
            when(ocorrenciaService.atualizarStatusOcorrencia(eq(OCORRENCIA_ID), any(AtualizarStatusOcorrenciaCommand.class)))
                    .thenReturn(detalhe);

            // Act
            ResponseEntity<OcorrenciaDetalheResponse> response =
                    ocorrenciaController.atualizarStatusOcorrencia(OCORRENCIA_ID, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(OCORRENCIA_ID);
            verify(ocorrenciaService).atualizarStatusOcorrencia(eq(OCORRENCIA_ID), any(AtualizarStatusOcorrenciaCommand.class));
        }

        @Test
        @DisplayName("Deve retornar 201 Created com header Location ao validar ocorrência")
        void deveRetornar201CreatedAoValidarOcorrencia() {
            // Arrange
            ValidacaoOcorrenciaRequest request = new ValidacaoOcorrenciaRequest(TipoValidacao.PRESENCIEI);

            // Act
            ResponseEntity<Void> response = ocorrenciaController.validarOcorrencia(OCORRENCIA_ID, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getHeaders().getLocation())
                    .hasToString("http://localhost/v1/ocorrencias/" + OCORRENCIA_ID + "/validacoes");
            verify(ocorrenciaService).validarOcorrencia(eq(OCORRENCIA_ID), any(ValidarOcorrenciaCommand.class));
        }

        @Test
        @DisplayName("Deve retornar 204 No Content ao cancelar validação comunitária")
        void deveRetornar204NoContentAoCancelarValidacao() {
            // Arrange & Act
            ResponseEntity<Void> response = ocorrenciaController.cancelarValidacaoOcorrencia(OCORRENCIA_ID);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(ocorrenciaService).cancelarValidacaoOcorrencia(OCORRENCIA_ID);
        }
    }
}
