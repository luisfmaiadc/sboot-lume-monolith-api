package br.com.lume.api.estabelecimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.estabelecimento.command.AtualizacaoEstabelecimentoCommand;
import br.com.lume.api.estabelecimento.command.CriacaoEstabelecimentoCommand;
import br.com.lume.api.estabelecimento.domain.Estabelecimento;
import br.com.lume.api.estabelecimento.domain.EstabelecimentoGestor;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoDetalheDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoPageDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.estabelecimento.mapper.EstabelecimentoMapper;
import br.com.lume.api.estabelecimento.repository.EstabelecimentoGestorRepository;
import br.com.lume.api.estabelecimento.repository.EstabelecimentoRepository;
import br.com.lume.api.model.AtualizacaoEstabelecimentoRequest;
import br.com.lume.api.model.CriacaoEstabelecimentoRequest;
import br.com.lume.api.shared.config.GeoProperties;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ForbiddenOperationException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.port.EstatisticasOcorrenciaEstabelecimentoDto;
import br.com.lume.api.shared.port.OcorrenciaEstabelecimentoQueryPort;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import br.com.lume.api.shared.security.AuthenticatedUserProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstabelecimentoService - Testes Unitários")
class EstabelecimentoServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);
    private static final UUID USUARIO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String CNPJ_VALIDO = "11222333000181";

    @Mock
    private EstabelecimentoRepository estabelecimentoRepository;

    @Mock
    private EstabelecimentoGestorRepository estabelecimentoGestorRepository;

    @Mock
    private CatalogoInternalFacade catalogoInternalFacade;

    @Mock
    private OcorrenciaEstabelecimentoQueryPort ocorrenciaEstabelecimentoQueryPort;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private EstabelecimentoMapper estabelecimentoMapper;
    private EstabelecimentoService service;

    @BeforeEach
    void setUp() {
        GeoProperties geoProperties = new GeoProperties(
                new GeoProperties.DiagnosticoGeoProperties(300, 500, 500),
                new GeoProperties.MapaGeoProperties(100, 500, 1000, 500, 365),
                new GeoProperties.EstabelecimentoGeoProperties(100, 1000, 5000)
        );
        estabelecimentoMapper = Mappers.getMapper(EstabelecimentoMapper.class);
        service = new EstabelecimentoService(
                estabelecimentoRepository,
                estabelecimentoGestorRepository,
                catalogoInternalFacade,
                ocorrenciaEstabelecimentoQueryPort,
                authenticatedUserProvider,
                geoProperties,
                FIXED_CLOCK,
                estabelecimentoMapper
        );
    }

    @Nested
    @DisplayName("autocompleteEstabelecimentos")
    class Autocomplete {

        @Test
        @DisplayName("Deve buscar sugestões de autocomplete e enriquecer com nome da categoria")
        void deveBuscarAutocompleteEEnriquecerCategoria() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);

            when(estabelecimentoRepository.buscarAutocomplete("Bar", "São Paulo", PageRequest.of(0, 5)))
                    .thenReturn(List.of(est));
            when(catalogoInternalFacade.listarCategoriasPorIds(Set.of(1)))
                    .thenReturn(List.of(catDto));

            List<EstabelecimentoResumoDto> resultado =
                    service.autocompleteEstabelecimentos("  Bar  ", "  São Paulo  ", null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().id()).isEqualTo(est.getId());
            assertThat(resultado.getFirst().nome()).isEqualTo("Bar do Zé");
            assertThat(resultado.getFirst().categoriaNome()).isEqualTo("Bares e Restaurantes");
            assertThat(resultado.getFirst().numero()).isEqualTo("1200");
        }

        @Test
        @DisplayName("Deve retornar lista vazia sem acionar catálogo quando nenhuma sugestão for encontrada")
        void deveRetornarListaVaziaQuandoSemResultados() {
            when(estabelecimentoRepository.buscarAutocomplete("Bar", null, PageRequest.of(0, 10)))
                    .thenReturn(List.of());

            List<EstabelecimentoResumoDto> resultado =
                    service.autocompleteEstabelecimentos("Bar", "   ", 10);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(catalogoInternalFacade);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando termo tiver menos de 2 caracteres ou limite for inválido")
        void deveRejeitarTermoCurtoOuLimiteInvalido() {
            assertThatThrownBy(() -> service.autocompleteEstabelecimentos(null, null, 5))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O termo de busca do autocomplete deve conter no mínimo 2 caracteres.");

            assertThatThrownBy(() -> service.autocompleteEstabelecimentos(" a ", null, 5))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O termo de busca do autocomplete deve conter no mínimo 2 caracteres.");

            assertThatThrownBy(() -> service.autocompleteEstabelecimentos("Bar", null, 0))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O limite do autocomplete deve estar entre 1 e 20.");

            assertThatThrownBy(() -> service.autocompleteEstabelecimentos("Bar", null, 21))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O limite do autocomplete deve estar entre 1 e 20.");
        }
    }

    @Nested
    @DisplayName("listarEstabelecimentos")
    class ListarEstabelecimentos {

        @Test
        @DisplayName("Deve listar estabelecimentos sem coordenadas usando listarComFiltros")
        void deveListarSemCoordenadas() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);
            PageImpl<Estabelecimento> pageResult = new PageImpl<>(List.of(est), PageRequest.of(0, 20), 1);

            when(estabelecimentoRepository.listarComFiltros("Bar", 1, "São Paulo", "Consolação", PageRequest.of(0, 20)))
                    .thenReturn(pageResult);
            when(catalogoInternalFacade.listarCategoriasPorIds(Set.of(1))).thenReturn(List.of(catDto));

            EstabelecimentoPageDto resultado = service.listarEstabelecimentos(
                    " Bar ", 1, " São Paulo ", " Consolação ", null, null, null, null, null
            );

            assertThat(resultado.content()).hasSize(1);
            assertThat(resultado.totalElements()).isEqualTo(1L);
            assertThat(resultado.content().getFirst().categoriaNome()).isEqualTo("Bares e Restaurantes");
            assertThat(resultado.content().getFirst().logradouro()).isEqualTo("Rua Augusta");
        }

        @Test
        @DisplayName("Deve listar estabelecimentos com coordenadas usando listarComFiltrosEspaciais e raio padrão quando nulo")
        void deveListarComCoordenadasEspaciais() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);
            PageImpl<Estabelecimento> pageResult = new PageImpl<>(List.of(est), PageRequest.of(0, 20), 1);

            when(estabelecimentoRepository.listarComFiltrosEspaciais(
                    null, null, null, null, -23.55052, -46.633308, 1000, PageRequest.of(0, 20)
            )).thenReturn(pageResult);
            when(catalogoInternalFacade.listarCategoriasPorIds(Set.of(1))).thenReturn(List.of(catDto));

            EstabelecimentoPageDto resultado = service.listarEstabelecimentos(
                    null, null, null, null, -23.55052, -46.633308, null, 0, 20
            );

            assertThat(resultado.content()).hasSize(1);
            verify(estabelecimentoRepository).listarComFiltrosEspaciais(
                    null, null, null, null, -23.55052, -46.633308, 1000, PageRequest.of(0, 20)
            );
        }

        @Test
        @DisplayName("Deve rejeitar XOR de coordenadas, raio fora da faixa e paginação inválida")
        void deveRejeitarParametrosInvalidosNaListagem() {
            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, -23.55, null, 1000, 0, 20
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Latitude e longitude devem ser informadas em conjunto para busca geográfica.");

            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, null, -46.63, 1000, 0, 20
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Latitude e longitude devem ser informadas em conjunto para busca geográfica.");

            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, -23.55, -46.63, 0, 0, 20
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Raio de busca inválido: deve ser positivo e não exceder o limite máximo configurado.");

            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, -23.55, -46.63, 10001, 0, 20
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Raio de busca inválido: deve ser positivo e não exceder o limite máximo configurado.");

            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, null, null, null, -1, 20
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O número da página não pode ser negativo.");

            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, null, null, null, 0, 0
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O tamanho da página deve estar entre 1 e 100.");

            assertThatThrownBy(() -> service.listarEstabelecimentos(
                    null, null, null, null, null, null, null, 0, 101
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O tamanho da página deve estar entre 1 e 100.");
        }
    }

    @Nested
    @DisplayName("cadastrarEstabelecimento")
    class CadastrarEstabelecimento {

        @Test
        @DisplayName("Deve cadastrar estabelecimento por cidadão comum sem criar vínculo de gestor")
        void deveCadastrarPorUsuarioComumSemVinculoDeGestor() {
            CriacaoEstabelecimentoRequest request = criarRequestCadastro();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(1)).thenReturn(catDto);
            when(estabelecimentoRepository.existsByCnpj(CNPJ_VALIDO)).thenReturn(false);
            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(false);

            EstabelecimentoDetalheDto response = service.cadastrarEstabelecimento(estabelecimentoMapper.toCommand(request));

            verify(estabelecimentoRepository).save(any(Estabelecimento.class));
            verifyNoInteractions(estabelecimentoGestorRepository);
            assertThat(response.nome()).isEqualTo("Bar do Zé");
            assertThat(response.categoria().nome()).isEqualTo("Bares e Restaurantes");
            assertThat(response.scoreReputacao()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(response.totalOcorrenciasHistoricas()).isZero();
        }

        @Test
        @DisplayName("Deve cadastrar estabelecimento por ROLE_GESTOR criando vínculo automático (RN12)")
        void deveCadastrarPorGestorCriandoVinculoAutomatico() {
            CriacaoEstabelecimentoRequest request = criarRequestCadastro();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(1)).thenReturn(catDto);
            when(estabelecimentoRepository.existsByCnpj(CNPJ_VALIDO)).thenReturn(false);
            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(true);

            EstabelecimentoDetalheDto response = service.cadastrarEstabelecimento(estabelecimentoMapper.toCommand(request));

            ArgumentCaptor<EstabelecimentoGestor> gestorCaptor = ArgumentCaptor.forClass(EstabelecimentoGestor.class);
            verify(estabelecimentoGestorRepository).save(gestorCaptor.capture());

            EstabelecimentoGestor gestorSalvo = gestorCaptor.getValue();
            assertThat(gestorSalvo.getEstabelecimentoId()).isEqualTo(response.id());
            assertThat(gestorSalvo.getUsuarioId()).isEqualTo(USUARIO_ID);
            assertThat(gestorSalvo.getCargo()).isEqualTo("GESTOR_RESPONSAVEL");
            assertThat(gestorSalvo.isAtivo()).isTrue();
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando CNPJ já estiver cadastrado")
        void deveLancarConflictQuandoCnpjDuplicado() {
            CriacaoEstabelecimentoRequest request = criarRequestCadastro();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(1)).thenReturn(catDto);
            when(estabelecimentoRepository.existsByCnpj(CNPJ_VALIDO)).thenReturn(true);

            assertThatThrownBy(() -> service.cadastrarEstabelecimento(estabelecimentoMapper.toCommand(request)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Já existe um estabelecimento cadastrado com o CNPJ informado.");

            verify(estabelecimentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar request nulo ou coordenadas nulas no cadastro")
        void deveRejeitarRequestOuCoordenadasNulas() {
            assertThatThrownBy(() -> service.cadastrarEstabelecimento((CriacaoEstabelecimentoCommand) null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Dados de criação do estabelecimento são obrigatórios.");

            CriacaoEstabelecimentoRequest semLat = criarRequestCadastro();
            semLat.setLatitude(null);
            assertThatThrownBy(() -> service.cadastrarEstabelecimento(estabelecimentoMapper.toCommand(semLat)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Coordenadas de latitude e longitude são obrigatórias.");

            CriacaoEstabelecimentoRequest semLon = criarRequestCadastro();
            semLon.setLongitude(null);
            assertThatThrownBy(() -> service.cadastrarEstabelecimento(estabelecimentoMapper.toCommand(semLon)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Coordenadas de latitude e longitude são obrigatórias.");
        }
    }

    @Nested
    @DisplayName("obterEstabelecimentoPorId, listarOcorrenciasEstabelecimento e obterMeuEstabelecimento")
    class ConsultasEMeuEstabelecimento {

        @Test
        @DisplayName("Deve obter estabelecimento por ID consolidando categoria e estatísticas de ocorrências")
        void deveObterEstabelecimentoPorIdComEstatisticas() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            EstatisticasOcorrenciaEstabelecimentoDto stats =
                    new EstatisticasOcorrenciaEstabelecimentoDto(7, 3, 1, 1, 2);

            when(estabelecimentoRepository.findById(est.getId())).thenReturn(Optional.of(est));
            when(catalogoInternalFacade.buscarCategoriaPorId(1)).thenReturn(Optional.empty()); // exercita fallback "OUTRO"
            when(ocorrenciaEstabelecimentoQueryPort.obterEstatisticasPorEstabelecimento(est.getId()))
                    .thenReturn(stats);

            EstabelecimentoDetalheDto response = service.obterEstabelecimentoPorId(est.getId());

            assertThat(response.id()).isEqualTo(est.getId());
            assertThat(response.categoria().codigo()).isEqualTo("OUTRO");
            assertThat(response.totalOcorrenciasHistoricas()).isEqualTo(7);
            assertThat(response.distribuicaoTurno().madrugada()).isEqualTo(3);
            assertThat(response.distribuicaoTurno().noite()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve listar ocorrências do estabelecimento ou lançar ResourceNotFoundException se não existir")
        void deveListarOcorrenciasOuLancar404() {
            UUID id = UUID.randomUUID();
            OcorrenciaResumoPagePortDto pageResult = new OcorrenciaResumoPagePortDto(List.of(), 0L, 0, 0, 20);

            when(estabelecimentoRepository.existsById(id)).thenReturn(true);
            when(ocorrenciaEstabelecimentoQueryPort.listarPorEstabelecimento(id, 1, 2, "NOITE", 0, 20))
                    .thenReturn(pageResult);

            OcorrenciaResumoPagePortDto resultado =
                    service.listarOcorrenciasEstabelecimento(id, 1, 2, "NOITE", 0, 20);

            assertThat(resultado).isEqualTo(pageResult);

            when(estabelecimentoRepository.existsById(id)).thenReturn(false);
            assertThatThrownBy(() -> service.listarOcorrenciasEstabelecimento(id, null, null, null, 0, 20))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Estabelecimento não encontrado.");

            assertThatThrownBy(() -> service.listarOcorrenciasEstabelecimento(null, null, null, null, 0, 20))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar ForbiddenOperationException em obterMeuEstabelecimento quando usuário não tiver ROLE_GESTOR")
        void deveNegarObterMeuEstabelecimentoSemRoleGestor() {
            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(false);

            assertThatThrownBy(() -> service.obterMeuEstabelecimento())
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Acesso restrito a usuários com perfil de gestor.");
        }

        @Test
        @DisplayName("Deve retornar Optional com detalhe quando gestor possuir vínculo ativo e Optional.empty quando não possuir")
        void deveRetornarMeuEstabelecimentoParaGestor() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            EstabelecimentoGestor vinculo = EstabelecimentoGestor.vincular(
                    est.getId(), USUARIO_ID, "GESTOR", USUARIO_ID, AGORA
            );

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(true);
            when(estabelecimentoGestorRepository.findFirstByIdUsuarioIdAndAtivoTrueOrderByVinculadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.of(vinculo));
            when(estabelecimentoRepository.findById(est.getId())).thenReturn(Optional.of(est));
            when(catalogoInternalFacade.buscarCategoriaPorId(1))
                    .thenReturn(Optional.of(new CategoriaEstabelecimentoDto(1, "BAR", "Bares", true)));
            when(ocorrenciaEstabelecimentoQueryPort.obterEstatisticasPorEstabelecimento(est.getId()))
                    .thenReturn(new EstatisticasOcorrenciaEstabelecimentoDto(0, 0, 0, 0, 0));

            Optional<EstabelecimentoDetalheDto> comVinculo = service.obterMeuEstabelecimento();
            assertThat(comVinculo).isPresent();
            assertThat(comVinculo.get().id()).isEqualTo(est.getId());

            when(estabelecimentoGestorRepository.findFirstByIdUsuarioIdAndAtivoTrueOrderByVinculadoEmDesc(USUARIO_ID))
                    .thenReturn(Optional.empty());

            assertThat(service.obterMeuEstabelecimento()).isEmpty();
        }
    }

    @Nested
    @DisplayName("atualizarEstabelecimento")
    class AtualizarEstabelecimento {

        @Test
        @DisplayName("Deve atualizar estabelecimento com sucesso quando gestor for o responsável")
        void deveAtualizarEstabelecimentoComSucesso() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            AtualizacaoEstabelecimentoRequest request = new AtualizacaoEstabelecimentoRequest()
                    .nome("Bar do Zé Renovado")
                    .categoriaId(2)
                    .cnpj("11444777000161")
                    .ativo(false);
            CategoriaEstabelecimentoDto novaCategoria =
                    new CategoriaEstabelecimentoDto(2, "RESTAURANTE", "Restaurantes", true);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(true);
            when(estabelecimentoRepository.findById(est.getId())).thenReturn(Optional.of(est));
            when(estabelecimentoGestorRepository.existsByIdEstabelecimentoIdAndIdUsuarioIdAndAtivoTrue(
                    est.getId(), USUARIO_ID
            )).thenReturn(true);
            when(catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(2)).thenReturn(novaCategoria);
            when(estabelecimentoRepository.existsByCnpjAndIdNot("11444777000161", est.getId())).thenReturn(false);
            when(ocorrenciaEstabelecimentoQueryPort.obterEstatisticasPorEstabelecimento(est.getId()))
                    .thenReturn(new EstatisticasOcorrenciaEstabelecimentoDto(0, 0, 0, 0, 0));

            EstabelecimentoDetalheDto response = service.atualizarEstabelecimento(est.getId(), estabelecimentoMapper.toCommand(request));

            verify(estabelecimentoRepository).save(est);
            assertThat(response.nome()).isEqualTo("Bar do Zé Renovado");
            assertThat(response.categoria().id()).isEqualTo(2);
            assertThat(response.cnpj()).isEqualTo("11444777000161");
            assertThat(response.logradouro()).isEqualTo("Rua Augusta");
            assertThat(response.ativo()).isFalse();
        }

        @Test
        @DisplayName("Deve rejeitar atualização quando request nulo, sem ROLE_GESTOR, sem vínculo ativo ou CNPJ em conflito")
        void deveRejeitarAtualizacaoQuandoRegrasVioladas() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            AtualizacaoEstabelecimentoRequest request = new AtualizacaoEstabelecimentoRequest()
                    .nome("Novo Nome")
                    .cnpj("11444777000161");

            assertThatThrownBy(() -> service.atualizarEstabelecimento(est.getId(), (AtualizacaoEstabelecimentoCommand) null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Dados de atualização do estabelecimento são obrigatórios.");

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(USUARIO_ID);
            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(false);

            assertThatThrownBy(() -> service.atualizarEstabelecimento(est.getId(), estabelecimentoMapper.toCommand(request)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Acesso restrito a usuários com perfil de gestor.");

            when(authenticatedUserProvider.possuiRole(EstabelecimentoService.ROLE_GESTOR)).thenReturn(true);
            when(estabelecimentoRepository.findById(est.getId())).thenReturn(Optional.of(est));
            when(estabelecimentoGestorRepository.existsByIdEstabelecimentoIdAndIdUsuarioIdAndAtivoTrue(
                    est.getId(), USUARIO_ID
            )).thenReturn(false);

            assertThatThrownBy(() -> service.atualizarEstabelecimento(est.getId(), estabelecimentoMapper.toCommand(request)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Usuário não possui vínculo ativo de gestão para este estabelecimento.");

            when(estabelecimentoGestorRepository.existsByIdEstabelecimentoIdAndIdUsuarioIdAndAtivoTrue(
                    est.getId(), USUARIO_ID
            )).thenReturn(true);
            when(catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(1))
                    .thenReturn(new CategoriaEstabelecimentoDto(1, "BAR", "Bares", true));
            when(estabelecimentoRepository.existsByCnpjAndIdNot("11444777000161", est.getId())).thenReturn(true);

            assertThatThrownBy(() -> service.atualizarEstabelecimento(est.getId(), estabelecimentoMapper.toCommand(request)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Já existe outro estabelecimento cadastrado com o CNPJ informado.");
        }
    }

    @Nested
    @DisplayName("EstabelecimentoInternalFacade")
    class FacadeMethods {

        @Test
        @DisplayName("Deve buscar estabelecimento ativo ou falhar com ResourceNotFoundException quando inativo ou ausente")
        void deveBuscarAtivoOuFalhar() {
            Estabelecimento estAtivo = criarEstabelecimentoPadrao();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares", true);

            when(estabelecimentoRepository.findById(estAtivo.getId())).thenReturn(Optional.of(estAtivo));
            when(catalogoInternalFacade.buscarCategoriaPorId(1)).thenReturn(Optional.of(catDto));

            EstabelecimentoResumoDto dto = service.buscarAtivoOuFalhar(estAtivo.getId());
            assertThat(dto.id()).isEqualTo(estAtivo.getId());
            assertThat(dto.nome()).isEqualTo("Bar do Zé");

            Estabelecimento estInativo = Estabelecimento.criar(
                    1, "Inativo", null, "Rua", "1", "Bairro", "Cidade", "UF",
                    -23.0, -46.0, USUARIO_ID, AGORA
            );
            estInativo.atualizarDadosCadastrais(
                    estInativo.getNome(),
                    estInativo.getCategoriaId(),
                    estInativo.getCnpj(),
                    estInativo.getLogradouro(),
                    estInativo.getNumero(),
                    estInativo.getBairro(),
                    estInativo.getCidade(),
                    estInativo.getEstado(),
                    false,
                    USUARIO_ID,
                    AGORA
            );
            when(estabelecimentoRepository.findById(estInativo.getId())).thenReturn(Optional.of(estInativo));

            UUID idInativo = estInativo.getId();
            assertThatThrownBy(() -> service.buscarAtivoOuFalhar(idInativo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Estabelecimento informado está inativo ou não foi encontrado.");

            UUID idInexistente = UUID.randomUUID();
            when(estabelecimentoRepository.findById(idInexistente)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.buscarAtivoOuFalhar(idInexistente))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Estabelecimento informado está inativo ou não foi encontrado.");
        }

        @Test
        @DisplayName("Deve exercitar métodos de busca simples do internal facade")
        void deveExercitarMetodosDeBuscaSimples() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            CategoriaEstabelecimentoDto catDto = new CategoriaEstabelecimentoDto(1, "BAR", "Bares", true);

            when(estabelecimentoRepository.findById(est.getId())).thenReturn(Optional.of(est));

            assertThat(service.buscarPorId(est.getId())).isPresent();

            assertThat(service.listarPorIds(null)).isEmpty();
            assertThat(service.listarPorIds(List.of())).isEmpty();

            when(estabelecimentoRepository.findAllById(List.of(est.getId()))).thenReturn(List.of(est));
            when(catalogoInternalFacade.listarCategoriasPorIds(Set.of(1))).thenReturn(List.of(catDto));
            assertThat(service.listarPorIds(List.of(est.getId()))).hasSize(1);

            UUID idVazio = UUID.randomUUID();
            when(estabelecimentoRepository.findAllById(List.of(idVazio))).thenReturn(List.of());
            assertThat(service.listarPorIds(List.of(idVazio))).isEmpty();

            assertThat(service.existeAtivoPorId(null)).isFalse();
            assertThat(service.existeAtivoPorId(est.getId())).isTrue();
        }
    }

    private Estabelecimento criarEstabelecimentoPadrao() {
        return Estabelecimento.criar(
                1,
                "Bar do Zé",
                CNPJ_VALIDO,
                "Rua Augusta",
                "1200",
                "Consolação",
                "São Paulo",
                "SP",
                -23.55052,
                -46.633308,
                USUARIO_ID,
                AGORA
        );
    }

    private CriacaoEstabelecimentoRequest criarRequestCadastro() {
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
                .cnpj(CNPJ_VALIDO)
                .numero("1200");
    }
}
