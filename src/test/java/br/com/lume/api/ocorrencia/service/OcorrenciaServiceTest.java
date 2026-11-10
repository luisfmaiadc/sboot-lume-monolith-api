package br.com.lume.api.ocorrencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.estabelecimento.api.EstabelecimentoInternalFacade;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.identidade.api.IdentidadeInternalFacade;
import br.com.lume.api.model.RegistroOcorrenciaRequest;
import br.com.lume.api.ocorrencia.command.AtualizarStatusOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.RegistrarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.ValidarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.domain.StatusOcorrencia;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;
import br.com.lume.api.ocorrencia.domain.TipoValidacao;
import br.com.lume.api.ocorrencia.domain.ValidacaoOcorrencia;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaPageDto;
import br.com.lume.api.ocorrencia.dto.OcorrenciaDetalheDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.repository.OcorrenciaRepository;
import br.com.lume.api.ocorrencia.repository.ValidacaoOcorrenciaRepository;
import br.com.lume.api.shared.event.ReputacaoEstabelecimentoRecalculadaEvent;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ForbiddenOperationException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.security.AuthenticatedUserProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcorrenciaService - Testes Unitários")
class OcorrenciaServiceTest {

    private static final Instant INSTANT_FIXO = Instant.parse("2026-05-10T14:00:00Z");
    private static final OffsetDateTime AGORA = OffsetDateTime.ofInstant(INSTANT_FIXO, ZoneOffset.UTC);
    private static final LocalDate DATA_HOJE = LocalDate.of(2026, 5, 10);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private static final UUID USUARIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_USUARIO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;

    @Mock
    private ValidacaoOcorrenciaRepository validacaoOcorrenciaRepository;

    @Mock
    private CatalogoInternalFacade catalogoInternalFacade;

    @Mock
    private EstabelecimentoInternalFacade estabelecimentoInternalFacade;

    @Mock
    private IdentidadeInternalFacade identidadeInternalFacade;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OcorrenciaMapper ocorrenciaMapper;
    private OcorrenciaService ocorrenciaService;

    @BeforeEach
    void setUp() {
        Clock clockFixo = Clock.fixed(INSTANT_FIXO, ZoneOffset.UTC);
        ocorrenciaMapper = Mappers.getMapper(OcorrenciaMapper.class);
        ocorrenciaService = new OcorrenciaService(
                ocorrenciaRepository,
                validacaoOcorrenciaRepository,
                catalogoInternalFacade,
                estabelecimentoInternalFacade,
                identidadeInternalFacade,
                authenticatedUserProvider,
                eventPublisher,
                clockFixo,
                ZONA_NEGOCIO,
                ocorrenciaMapper
        );
    }

    private TipoViolenciaDto criarTipoViolenciaDto() {
        return new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);
    }

    private MarcadorVulnerabilidadeDto criarMarcadorDto() {
        return new MarcadorVulnerabilidadeDto(2, "POUCA_ILUMINACAO", "Pouca Iluminação", "Local escuro", true);
    }

    private EstabelecimentoResumoDto criarEstabelecimentoResumoDto() {
        return new EstabelecimentoResumoDto(
                ESTABELECIMENTO_ID,
                1,
                "Bar",
                "Bar Lume",
                "Rua Augusta",
                "100",
                "Consolação",
                "São Paulo",
                "SP",
                -23.55,
                -46.65,
                new BigDecimal("5.00"),
                true,
                true
        );
    }

    private Ocorrencia criarOcorrenciaDominio(UUID estabelecimentoId, UUID autorId) {
        return Ocorrencia.registrar(
                estabelecimentoId,
                -23.55,
                -46.65,
                "Rua Augusta",
                "100",
                "Consolação",
                "São Paulo",
                "SP",
                DATA_HOJE.minusDays(1),
                FaixaHorario.NOITE,
                1,
                2,
                estabelecimentoId != null ? TipoAmbiente.ESTABELECIMENTO : TipoAmbiente.VIA_PUBLICA,
                autorId,
                AGORA
        );
    }

    @Nested
    @DisplayName("registrarOcorrencia")
    class RegistrarOcorrencia {

        @Test
        @DisplayName("Deve registrar ocorrência vinculada a estabelecimento herdando endereço e coordenadas do estabelecimento")
        void deveRegistrarOcorrenciaComEstabelecimentoEPublicarEvento() {
            // Arrange
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    DATA_HOJE.minusDays(1),
                    br.com.lume.api.model.FaixaHorario.NOITE,
                    1
            );
            request.setEstabelecimentoId(ESTABELECIMENTO_ID);
            request.setMarcadorVulnerabilidadeId(2);

            TipoViolenciaDto tvDto = criarTipoViolenciaDto();
            MarcadorVulnerabilidadeDto marcadorDto = criarMarcadorDto();
            EstabelecimentoResumoDto estabDto = criarEstabelecimentoResumoDto();

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(catalogoInternalFacade.buscarTipoViolenciaAtivoOuFalhar(1)).thenReturn(tvDto);
            when(catalogoInternalFacade.buscarMarcadorAtivoOuFalhar(2)).thenReturn(marcadorDto);
            when(estabelecimentoInternalFacade.buscarAtivoOuFalhar(ESTABELECIMENTO_ID)).thenReturn(estabDto);

            // Act
            OcorrenciaDetalheDto response = ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request));

            // Assert
            assertThat(response.id()).isNotNull();
            assertThat(response.estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
            assertThat(response.estabelecimentoNome()).isEqualTo("Bar Lume");
            assertThat(response.tipoViolencia().nome()).isEqualTo("Assédio Verbal");
            assertThat(response.marcadorVulnerabilidade().nome()).isEqualTo("Pouca Iluminação");

            ArgumentCaptor<Ocorrencia> ocorrenciaCaptor = ArgumentCaptor.forClass(Ocorrencia.class);
            verify(ocorrenciaRepository).save(ocorrenciaCaptor.capture());
            Ocorrencia salva = ocorrenciaCaptor.getValue();
            assertThat(salva.getLatitude()).isEqualTo(estabDto.latitude());
            assertThat(salva.getLongitude()).isEqualTo(estabDto.longitude());
            assertThat(salva.getLogradouro()).isEqualTo(estabDto.logradouro());
            assertThat(salva.getNumero()).isEqualTo(estabDto.numero());
            assertThat(salva.getBairro()).isEqualTo(estabDto.bairro());
            assertThat(salva.getCidade()).isEqualTo(estabDto.cidade());
            assertThat(salva.getEstado()).isEqualTo(estabDto.estado());
            assertThat(salva.getTipoAmbiente()).isEqualTo(TipoAmbiente.ESTABELECIMENTO);

            ArgumentCaptor<ReputacaoEstabelecimentoRecalculadaEvent> eventCaptor =
                    ArgumentCaptor.forClass(ReputacaoEstabelecimentoRecalculadaEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
            assertThat(eventCaptor.getValue().ocorridoEm()).isEqualTo(INSTANT_FIXO);
        }

        @Test
        @DisplayName("Deve registrar ocorrência em via pública sem disparar evento de reputação")
        void deveRegistrarOcorrenciaEmViaPublicaSemDispararEvento() {
            // Arrange
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    DATA_HOJE,
                    br.com.lume.api.model.FaixaHorario.TARDE,
                    1
            );
            request.setLatitude(-23.551234);
            request.setLongitude(-46.651234);
            request.setLogradouro("Rua Augusta");
            request.setBairro("Consolação");
            request.setCidade("São Paulo");
            request.setEstado("SP");
            request.setTipoAmbiente(br.com.lume.api.model.TipoAmbiente.VIA_PUBLICA);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(catalogoInternalFacade.buscarTipoViolenciaAtivoOuFalhar(1)).thenReturn(criarTipoViolenciaDto());

            // Act
            OcorrenciaDetalheDto response = ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request));

            // Assert
            assertThat(response.estabelecimentoId()).isNull();
            assertThat(response.marcadorVulnerabilidade()).isNull();
            // Coordenadas públicas anonimizadas para 3 casas decimais
            assertThat(response.latitude()).isEqualTo(-23.551);
            assertThat(response.longitude()).isEqualTo(-46.651);
            assertThat(response.logradouro()).isNull();
            assertThat(response.numero()).isNull();
            assertThat(response.bairro()).isNull();
            assertThat(response.dataOcorrencia()).isEqualTo(DATA_HOJE.withDayOfMonth(1));
            verify(ocorrenciaRepository).save(any(Ocorrencia.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando usuário atingir o limite de 5 ocorrências nas últimas 24 horas")
        void deveLancarBusinessRuleExceptionQuandoAtingirLimiteDiario() {
            // Arrange
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    DATA_HOJE, br.com.lume.api.model.FaixaHorario.NOITE, 1
            );
            request.setEstabelecimentoId(ESTABELECIMENTO_ID);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.countByCriadoPorAndCriadoEmAfter(USUARIO_ID, AGORA.minusHours(24)))
                    .thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Limite de 5 ocorrências registradas nas últimas 24 horas atingido.");
            verify(identidadeInternalFacade).adquirirLockParaUsuario(USUARIO_ID);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando request ou coordenadas forem nulas em via pública")
        void deveLancarBusinessRuleExceptionQuandoRequestOuCoordenadasNulas() {
            // Arrange
            RegistroOcorrenciaRequest semLatitude = new RegistroOcorrenciaRequest(
                    DATA_HOJE, br.com.lume.api.model.FaixaHorario.NOITE, 1
            );
            semLatitude.setLongitude(-46.65);
            semLatitude.setLogradouro("Rua A");
            semLatitude.setBairro("Bairro");
            semLatitude.setCidade("Cidade");
            semLatitude.setEstado("SP");
            semLatitude.setTipoAmbiente(br.com.lume.api.model.TipoAmbiente.VIA_PUBLICA);

            // Act & Assert
            assertThatThrownBy(() -> ocorrenciaService.registrarOcorrencia((RegistrarOcorrenciaCommand) null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Dados de registro da ocorrência são obrigatórios.");

            assertThatThrownBy(() -> ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(semLatitude)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Coordenadas de latitude e longitude são obrigatórias.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando ocorrência em via pública não informar tipo de ambiente")
        void deveLancarBusinessRuleExceptionQuandoViaPublicaSemTipoAmbiente() {
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    DATA_HOJE, br.com.lume.api.model.FaixaHorario.NOITE, 1
            );
            request.setLatitude(-23.55);
            request.setLongitude(-46.65);
            request.setLogradouro("Rua A");
            request.setBairro("Bairro");
            request.setCidade("Cidade");
            request.setEstado("SP");

            assertThatThrownBy(() -> ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Tipo de ambiente da ocorrência é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando tipo de ambiente for ESTABELECIMENTO mas sem estabelecimentoId")
        void deveLancarBusinessRuleExceptionQuandoAmbienteEstabelecimentoSemId() {
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    DATA_HOJE, br.com.lume.api.model.FaixaHorario.NOITE, 1
            );
            request.setLatitude(-23.55);
            request.setLongitude(-46.65);
            request.setLogradouro("Rua A");
            request.setBairro("Bairro");
            request.setCidade("Cidade");
            request.setEstado("SP");
            request.setTipoAmbiente(br.com.lume.api.model.TipoAmbiente.ESTABELECIMENTO);

            assertThatThrownBy(() -> ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Para tipo de ambiente ESTABELECIMENTO, o ID do estabelecimento deve ser informado.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando ocorrência com estabelecimento tiver tipo de ambiente diferente de ESTABELECIMENTO")
        void deveLancarBusinessRuleExceptionQuandoEstabelecimentoComTipoAmbienteDivergente() {
            RegistroOcorrenciaRequest request = new RegistroOcorrenciaRequest(
                    DATA_HOJE, br.com.lume.api.model.FaixaHorario.NOITE, 1
            );
            request.setEstabelecimentoId(ESTABELECIMENTO_ID);
            request.setTipoAmbiente(br.com.lume.api.model.TipoAmbiente.VIA_PUBLICA);

            when(catalogoInternalFacade.buscarTipoViolenciaAtivoOuFalhar(1)).thenReturn(criarTipoViolenciaDto());
            when(estabelecimentoInternalFacade.buscarAtivoOuFalhar(ESTABELECIMENTO_ID)).thenReturn(criarEstabelecimentoResumoDto());

            assertThatThrownBy(() -> ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Ocorrências vinculadas a estabelecimento devem ter tipo de ambiente ESTABELECIMENTO.");
        }
    }

    @Nested
    @DisplayName("obterOcorrenciaPorId")
    class ObterOcorrenciaPorId {

        @Test
        @DisplayName("Deve retornar detalhe da ocorrência publicada sem expor criadoPor")
        void deveRetornarDetalheDaOcorrenciaPublicada() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, USUARIO_ID);
            UUID id = ocorrencia.getId();

            when(ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrencia));
            when(catalogoInternalFacade.buscarTipoViolenciaPorId(1))
                    .thenReturn(Optional.of(criarTipoViolenciaDto()));
            when(catalogoInternalFacade.buscarMarcadorPorId(2))
                    .thenReturn(Optional.of(criarMarcadorDto()));
            when(estabelecimentoInternalFacade.buscarPorId(ESTABELECIMENTO_ID))
                    .thenReturn(Optional.of(criarEstabelecimentoResumoDto()));

            // Act
            OcorrenciaDetalheDto response = ocorrenciaService.obterOcorrenciaPorId(id);

            // Assert
            assertThat(response.id()).isEqualTo(id);
            assertThat(response.estabelecimentoNome()).isEqualTo("Bar Lume");
            assertThat(response.tipoViolencia().codigo()).isEqualTo("ASSEDIO_VERBAL");
            assertThat(response.marcadorVulnerabilidade().codigo()).isEqualTo("POUCA_ILUMINACAO");
            assertThat(response.dataRegistro()).isEqualTo(DATA_HOJE);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando id for nulo ou ocorrência não estiver publicada")
        void deveLancarResourceNotFoundExceptionQuandoNaoEncontrada() {
            UUID id = UUID.randomUUID();
            when(ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ocorrenciaService.obterOcorrenciaPorId(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Ocorrência não encontrada.");

            assertThatThrownBy(() -> ocorrenciaService.obterOcorrenciaPorId(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Ocorrência não encontrada.");
        }
    }

    @Nested
    @DisplayName("listarMinhasOcorrencias")
    class ListarMinhasOcorrencias {

        @Test
        @DisplayName("Deve listar ocorrências do usuário autenticado paginadas com flag permiteExclusao")
        void deveListarMinhasOcorrenciasPaginadas() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, USUARIO_ID);
            Page<Ocorrencia> pagina = new PageImpl<>(List.of(ocorrencia), PageRequest.of(0, 20), 1);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByCriadoPorOrderByCriadoEmDesc(USUARIO_ID, PageRequest.of(0, 20)))
                    .thenReturn(pagina);
            when(catalogoInternalFacade.listarTiposViolenciaPorIds(anyCollection()))
                    .thenReturn(List.of(criarTipoViolenciaDto()));
            when(estabelecimentoInternalFacade.listarPorIds(anyCollection()))
                    .thenReturn(List.of(criarEstabelecimentoResumoDto()));

            // Act
            MinhaOcorrenciaPageDto result = ocorrenciaService.listarMinhasOcorrencias(null, null);

            // Assert
            assertThat(result.totalElements()).isEqualTo(1L);
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(ocorrencia.getId());
            assertThat(result.content().getFirst().estabelecimentoNome()).isEqualTo("Bar Lume");
            assertThat(result.content().getFirst().permiteExclusao()).isTrue();
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando page ou size forem inválidos")
        void deveLancarBusinessRuleExceptionQuandoPaginacaoInvalida() {
            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);

            assertThatThrownBy(() -> ocorrenciaService.listarMinhasOcorrencias(-1, 20))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O número da página não pode ser negativo.");

            assertThatThrownBy(() -> ocorrenciaService.listarMinhasOcorrencias(0, 101))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O tamanho da página deve estar entre 1 e 100.");
        }
    }

    @Nested
    @DisplayName("excluirOcorrencia")
    class ExcluirOcorrencia {

        @Test
        @DisplayName("Deve excluir logicamente ocorrência de estabelecimento e publicar evento de recálculo")
        void deveExcluirLogicamenteOcorrenciaEPublicarEvento() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, USUARIO_ID);
            UUID id = ocorrencia.getId();

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findById(id)).thenReturn(Optional.of(ocorrencia));

            // Act
            ocorrenciaService.excluirOcorrencia(id);

            // Assert
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.REMOVIDA);
            verify(ocorrenciaRepository).save(ocorrencia);
            verify(eventPublisher).publishEvent(new ReputacaoEstabelecimentoRecalculadaEvent(ESTABELECIMENTO_ID, INSTANT_FIXO));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenOperationException quando solicitante não for o autor")
        void deveLancarForbiddenOperationExceptionQuandoNaoForAutor() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(null, OUTRO_USUARIO_ID);
            UUID id = ocorrencia.getId();

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findById(id)).thenReturn(Optional.of(ocorrencia));

            // Act & Assert
            assertThatThrownBy(() -> ocorrenciaService.excluirOcorrencia(id))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Nested
    @DisplayName("atualizarStatusOcorrencia (Moderação)")
    class ModeracaoStatus {

        @Test
        @DisplayName("Deve atualizar status da ocorrência como ADMIN e publicar evento de reputação")
        void deveAtualizarStatusComoAdmin() {
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, USUARIO_ID);
            UUID id = ocorrencia.getId();
            AtualizarStatusOcorrenciaCommand command = new AtualizarStatusOcorrenciaCommand(StatusOcorrencia.SOB_ANALISE);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findById(id)).thenReturn(Optional.of(ocorrencia));
            when(authenticatedUserProvider.possuiRole("ROLE_ADMIN")).thenReturn(true);
            when(catalogoInternalFacade.buscarTipoViolenciaPorId(1)).thenReturn(Optional.of(criarTipoViolenciaDto()));
            when(catalogoInternalFacade.buscarMarcadorPorId(2)).thenReturn(Optional.of(criarMarcadorDto()));
            when(estabelecimentoInternalFacade.buscarPorId(ESTABELECIMENTO_ID)).thenReturn(Optional.of(criarEstabelecimentoResumoDto()));

            OcorrenciaDetalheDto response = ocorrenciaService.atualizarStatusOcorrencia(id, command);

            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.SOB_ANALISE);
            assertThat(response.id()).isEqualTo(id);
            verify(ocorrenciaRepository).save(ocorrencia);
            verify(eventPublisher).publishEvent(new ReputacaoEstabelecimentoRecalculadaEvent(ESTABELECIMENTO_ID, INSTANT_FIXO));
        }

        @Test
        @DisplayName("Deve atualizar status como MODERADOR ou lançar Forbidden se tentar restaurar removida")
        void deveAtualizarStatusComoModerador() {
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, USUARIO_ID);
            UUID id = ocorrencia.getId();
            AtualizarStatusOcorrenciaCommand command = new AtualizarStatusOcorrenciaCommand(StatusOcorrencia.SOB_ANALISE);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findById(id)).thenReturn(Optional.of(ocorrencia));
            when(authenticatedUserProvider.possuiRole("ROLE_ADMIN")).thenReturn(false);
            when(authenticatedUserProvider.possuiRole("ROLE_MODERADOR")).thenReturn(true);
            when(catalogoInternalFacade.buscarTipoViolenciaPorId(1)).thenReturn(Optional.of(criarTipoViolenciaDto()));
            when(catalogoInternalFacade.buscarMarcadorPorId(2)).thenReturn(Optional.of(criarMarcadorDto()));
            when(estabelecimentoInternalFacade.buscarPorId(ESTABELECIMENTO_ID)).thenReturn(Optional.of(criarEstabelecimentoResumoDto()));

            OcorrenciaDetalheDto response = ocorrenciaService.atualizarStatusOcorrencia(id, command);
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.SOB_ANALISE);
            assertThat(response.id()).isEqualTo(id);

            // Moderador coloca em REMOVIDA
            ocorrenciaService.atualizarStatusOcorrencia(id, new AtualizarStatusOcorrenciaCommand(StatusOcorrencia.REMOVIDA));
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.REMOVIDA);

            // Se a ocorrência já for REMOVIDA, moderador não pode restaurar
            assertThatThrownBy(() -> ocorrenciaService.atualizarStatusOcorrencia(id, new AtualizarStatusOcorrenciaCommand(StatusOcorrencia.PUBLICADA)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Ocorrência removida não pode ser restaurada por moderadores.");
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException quando usuário comum tentar atualizar status")
        void deveLancarAccessDeniedParaUsuarioComum() {
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, USUARIO_ID);
            UUID id = ocorrencia.getId();
            AtualizarStatusOcorrenciaCommand command = new AtualizarStatusOcorrenciaCommand(StatusOcorrencia.SOB_ANALISE);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findById(id)).thenReturn(Optional.of(ocorrencia));
            when(authenticatedUserProvider.possuiRole("ROLE_ADMIN")).thenReturn(false);
            when(authenticatedUserProvider.possuiRole("ROLE_MODERADOR")).thenReturn(false);

            assertThatThrownBy(() -> ocorrenciaService.atualizarStatusOcorrencia(id, command))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Acesso negado");
        }
    }

    @Nested
    @DisplayName("validarOcorrencia e cancelarValidacaoOcorrencia")
    class ValidacaoComunitaria {

        @Test
        @DisplayName("Deve validar ocorrência de outro autor, incrementar contador atômico e publicar evento de reputação")
        void deveValidarOcorrenciaComSucesso() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, OUTRO_USUARIO_ID);
            UUID id = ocorrencia.getId();
            ValidarOcorrenciaCommand command = new ValidarOcorrenciaCommand(TipoValidacao.PRESENCIEI);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrencia));
            when(validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioId(id, USUARIO_ID)).thenReturn(Optional.empty());

            // Act
            ocorrenciaService.validarOcorrencia(id, command);

            // Assert
            verify(validacaoOcorrenciaRepository).save(any(ValidacaoOcorrencia.class));
            verify(ocorrenciaRepository).incrementarTotalValidacoes(id, AGORA);
            verify(eventPublisher).publishEvent(new ReputacaoEstabelecimentoRecalculadaEvent(ESTABELECIMENTO_ID, INSTANT_FIXO));
        }

        @Test
        @DisplayName("Deve reativar validação inativa com sucesso, atualizar tipo e incrementar contador atômico")
        void deveReativarValidacaoInativaComSucesso() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, OUTRO_USUARIO_ID);
            UUID id = ocorrencia.getId();
            ValidacaoOcorrencia validacaoInativa = ValidacaoOcorrencia.criar(
                    ocorrencia, USUARIO_ID, TipoValidacao.PRESENCIEI, AGORA
            );
            validacaoInativa.inativar(USUARIO_ID, AGORA);
            ValidarOcorrenciaCommand command = new ValidarOcorrenciaCommand(TipoValidacao.CONHECO_RISCO);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrencia));
            when(validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioId(id, USUARIO_ID))
                    .thenReturn(Optional.of(validacaoInativa));

            // Act
            ocorrenciaService.validarOcorrencia(id, command);

            // Assert
            assertThat(validacaoInativa.isAtivo()).isTrue();
            assertThat(validacaoInativa.getTipoValidacao()).isEqualTo(TipoValidacao.CONHECO_RISCO);
            assertThat(validacaoInativa.getAtualizadoPor()).isEqualTo(USUARIO_ID);
            assertThat(validacaoInativa.getAtualizadoEm()).isNotNull();
            verify(validacaoOcorrenciaRepository).save(validacaoInativa);
            verify(ocorrenciaRepository).incrementarTotalValidacoes(id, AGORA);
            verify(eventPublisher).publishEvent(new ReputacaoEstabelecimentoRecalculadaEvent(ESTABELECIMENTO_ID, INSTANT_FIXO));
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException ao tentar validar ocorrência criada há mais de 30 dias")
        void deveRejeitarValidacaoApos30DiasDaCriacao() {
            // Arrange
            Ocorrencia ocorrenciaAntiga = Ocorrencia.registrar(
                    ESTABELECIMENTO_ID,
                    -23.55,
                    -46.65,
                    "Rua Augusta",
                    "100",
                    "Consolação",
                    "São Paulo",
                    "SP",
                    DATA_HOJE.minusDays(45),
                    FaixaHorario.NOITE,
                    1,
                    2,
                    TipoAmbiente.ESTABELECIMENTO,
                    OUTRO_USUARIO_ID,
                    AGORA.minusDays(31)
            );
            ValidarOcorrenciaCommand command = new ValidarOcorrenciaCommand(TipoValidacao.PRESENCIEI);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByIdAndStatus(ocorrenciaAntiga.getId(), StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrenciaAntiga));

            // Act & Assert
            assertThatThrownBy(() -> ocorrenciaService.validarOcorrencia(ocorrenciaAntiga.getId(), command))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Ocorrências só podem receber validações comunitárias em até 30 dias após seu registro.");

            verify(validacaoOcorrenciaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando usuário já validou a ocorrência ou for o próprio autor")
        void deveLancarConflictExceptionEmValidacaoDuplicadaOuAutoValidacao() {
            // Arrange
            Ocorrencia ocorrenciaOutroAutor = criarOcorrenciaDominio(null, OUTRO_USUARIO_ID);
            Ocorrencia ocorrenciaProprioAutor = criarOcorrenciaDominio(null, USUARIO_ID);
            ValidacaoOcorrencia validacaoAtiva = ValidacaoOcorrencia.criar(
                    ocorrenciaOutroAutor, USUARIO_ID, TipoValidacao.PRESENCIEI, AGORA
            );
            ValidarOcorrenciaCommand command = new ValidarOcorrenciaCommand(TipoValidacao.PRESENCIEI);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByIdAndStatus(ocorrenciaOutroAutor.getId(), StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrenciaOutroAutor));
            when(validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioId(ocorrenciaOutroAutor.getId(), USUARIO_ID))
                    .thenReturn(Optional.of(validacaoAtiva));

            // Act & Assert - duplicada
            assertThatThrownBy(() -> ocorrenciaService.validarOcorrencia(ocorrenciaOutroAutor.getId(), command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("O usuário já validou esta ocorrência anteriormente.");

            // Arrange - auto-validação
            when(ocorrenciaRepository.findByIdAndStatus(ocorrenciaProprioAutor.getId(), StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrenciaProprioAutor));
            when(validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioId(ocorrenciaProprioAutor.getId(), USUARIO_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert - auto-validação
            assertThatThrownBy(() -> ocorrenciaService.validarOcorrencia(ocorrenciaProprioAutor.getId(), command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Operação negada: o autor não pode validar sua própria ocorrência.");
        }

        @Test
        @DisplayName("Deve cancelar validação comunitária existente através de exclusão lógica (inativação) e decrementar contador atômico")
        void deveCancelarValidacaoOcorrenciaComSucesso() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaDominio(ESTABELECIMENTO_ID, OUTRO_USUARIO_ID);
            UUID id = ocorrencia.getId();
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(
                    ocorrencia, USUARIO_ID, TipoValidacao.CONHECO_RISCO, AGORA
            );

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrencia));
            when(validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioIdAndAtivoTrue(id, USUARIO_ID))
                    .thenReturn(Optional.of(validacao));

            // Act
            ocorrenciaService.cancelarValidacaoOcorrencia(id);

            // Assert
            assertThat(validacao.isAtivo()).isFalse();
            assertThat(validacao.getAtualizadoPor()).isEqualTo(USUARIO_ID);
            assertThat(validacao.getAtualizadoEm()).isNotNull();
            verify(validacaoOcorrenciaRepository).save(validacao);
            verify(validacaoOcorrenciaRepository, never()).delete(any());
            verify(ocorrenciaRepository).decrementarTotalValidacoes(id, AGORA);
            verify(eventPublisher).publishEvent(new ReputacaoEstabelecimentoRecalculadaEvent(ESTABELECIMENTO_ID, INSTANT_FIXO));
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao cancelar validação inexistente ou já inativa")
        void deveLancarResourceNotFoundExceptionAoCancelarValidacaoInexistente() {
            Ocorrencia ocorrencia = criarOcorrenciaDominio(null, OUTRO_USUARIO_ID);
            UUID id = ocorrencia.getId();

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA))
                    .thenReturn(Optional.of(ocorrencia));
            when(validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioIdAndAtivoTrue(id, USUARIO_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> ocorrenciaService.cancelarValidacaoOcorrencia(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Validação comunitária não encontrada para esta ocorrência.");
        }
    }
}
