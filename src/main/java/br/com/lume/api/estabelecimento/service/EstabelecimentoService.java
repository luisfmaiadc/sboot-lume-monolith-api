package br.com.lume.api.estabelecimento.service;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.estabelecimento.api.EstabelecimentoInternalFacade;
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
import br.com.lume.api.shared.config.GeoProperties;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ForbiddenOperationException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.port.ContagemItemDto;
import br.com.lume.api.shared.port.EstatisticasOcorrenciaEstabelecimentoDto;
import br.com.lume.api.shared.port.OcorrenciaEstabelecimentoQueryPort;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import br.com.lume.api.shared.security.AuthenticatedUserProvider;
import br.com.lume.api.shared.util.CnpjValidator;
import br.com.lume.api.shared.util.GeoUtils;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstabelecimentoService implements EstabelecimentoInternalFacade {

    public static final String ROLE_GESTOR = "ROLE_GESTOR";
    private static final int LIMITE_AUTOCOMPLETE_PADRAO = 5;
    private static final int LIMITE_AUTOCOMPLETE_MAXIMO = 20;
    private static final int PAGE_PADRAO = 0;
    private static final int SIZE_PADRAO = 20;
    private static final int SIZE_MAXIMO = 100;

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EstabelecimentoGestorRepository estabelecimentoGestorRepository;
    private final CatalogoInternalFacade catalogoInternalFacade;
    private final OcorrenciaEstabelecimentoQueryPort ocorrenciaEstabelecimentoQueryPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final GeoProperties geoProperties;
    private final Clock clock;
    private final EstabelecimentoMapper estabelecimentoMapper;

    @Transactional(readOnly = true)
    public List<EstabelecimentoResumoDto> autocompleteEstabelecimentos(
            String termo,
            String cidade,
            Integer limite
    ) {
        validarTermoAutocomplete(termo);
        int limiteEfetivo = validarLimiteAutocomplete(limite);

        List<Estabelecimento> estabelecimentos = estabelecimentoRepository.buscarAutocomplete(
                termo.trim(),
                normalizarFiltro(cidade),
                PageRequest.of(0, limiteEfetivo)
        );
        if (estabelecimentos.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> nomesCategorias = carregarNomesCategorias(estabelecimentos);
        return estabelecimentoMapper.toResumoDtoList(estabelecimentos, nomesCategorias);
    }

    @Transactional(readOnly = true)
    public EstabelecimentoPageDto listarEstabelecimentos(
            String termo,
            Integer categoriaId,
            String cidade,
            String bairro,
            Double latitude,
            Double longitude,
            Integer raioMetros,
            Integer page,
            Integer size
    ) {
        validarCoordenadasEspaciais(latitude, longitude);
        PageRequest pageable = PageRequest.of(validarPage(page), validarSize(size));

        Page<Estabelecimento> pagina = buscarPaginaEstabelecimentos(
                termo, categoriaId, cidade, bairro, latitude, longitude, raioMetros, pageable
        );
        return construirPageDto(pagina);
    }

    @Transactional
    public EstabelecimentoDetalheDto cadastrarEstabelecimento(CriacaoEstabelecimentoCommand command) {
        validarRequestCriacao(command);

        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        CategoriaEstabelecimentoDto categoria = catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(command.categoriaId());
        String cnpjNormalizado = processarCnpjCadastro(command.cnpj());

        OffsetDateTime agora = OffsetDateTime.now(clock);
        Estabelecimento estabelecimento = criarEntidadeEstabelecimento(
                command, categoria.id(), cnpjNormalizado, usuarioId, agora
        );

        estabelecimentoRepository.save(estabelecimento);
        log.info("Estabelecimento cadastrado com sucesso: estabelecimentoId={}, usuarioId={}",
                estabelecimento.getId(), usuarioId);

        vincularGestorSeAplicavel(estabelecimento.getId(), usuarioId, agora);

        EstatisticasOcorrenciaEstabelecimentoDto statsIniciais =
                new EstatisticasOcorrenciaEstabelecimentoDto(0, 0, 0, 0, 0, Map.of());
        return estabelecimentoMapper.toDetalheDto(estabelecimento, categoria, statsIniciais, List.of());
    }

    @Transactional(readOnly = true)
    public EstabelecimentoDetalheDto obterEstabelecimentoPorId(UUID id) {
        Estabelecimento estabelecimento = buscarEntidadeOuFalhar(id);
        CategoriaEstabelecimentoDto categoria = resolverCategoria(estabelecimento.getCategoriaId());
        EstatisticasOcorrenciaEstabelecimentoDto estatisticas =
                ocorrenciaEstabelecimentoQueryPort.obterEstatisticasPorEstabelecimento(id);
        List<ContagemItemDto> distribuicaoTipoViolencia =
                construirDistribuicaoTipoViolencia(estatisticas.distribuicaoTipoViolencia());
        return estabelecimentoMapper.toDetalheDto(estabelecimento, categoria, estatisticas, distribuicaoTipoViolencia);
    }

    @Transactional(readOnly = true)
    public OcorrenciaResumoPagePortDto listarOcorrenciasEstabelecimento(
            UUID id,
            Integer tipoViolenciaId,
            Integer marcadorId,
            String faixaHorario,
            Integer page,
            Integer size
    ) {
        if (id == null || !estabelecimentoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Estabelecimento não encontrado.");
        }
        int pageEfetivo = validarPage(page);
        int sizeEfetivo = validarSize(size);
        return ocorrenciaEstabelecimentoQueryPort.listarPorEstabelecimento(
                id,
                tipoViolenciaId,
                marcadorId,
                faixaHorario,
                pageEfetivo,
                sizeEfetivo
        );
    }

    @Transactional(readOnly = true)
    public Optional<EstabelecimentoDetalheDto> obterMeuEstabelecimento() {
        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        validarPerfilGestor();

        return estabelecimentoGestorRepository
                .findFirstByIdUsuarioIdAndAtivoTrueOrderByVinculadoEmDesc(usuarioId)
                .map(EstabelecimentoGestor::getEstabelecimentoId)
                .map(this::obterEstabelecimentoPorId);
    }

    @Transactional
    public EstabelecimentoDetalheDto atualizarEstabelecimento(UUID id, AtualizacaoEstabelecimentoCommand command) {
        validarRequestAtualizacao(command);

        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        validarPerfilGestor();

        Estabelecimento estabelecimento = buscarEntidadeOuFalhar(id);
        validarVinculoGestao(id, usuarioId);

        Integer categoriaIdEfetiva = command.categoriaId() != null
                ? command.categoriaId()
                : estabelecimento.getCategoriaId();
        CategoriaEstabelecimentoDto categoria = catalogoInternalFacade.buscarCategoriaAtivaOuFalhar(categoriaIdEfetiva);

        String cnpjEfetivo = command.cnpj() != null
                ? normalizarCnpj(command.cnpj())
                : estabelecimento.getCnpj();
        validarCnpjParaAtualizacao(cnpjEfetivo, id);

        aplicarAtualizacaoDadosCadastrais(estabelecimento, command, categoria, cnpjEfetivo, usuarioId);
        estabelecimentoRepository.save(estabelecimento);

        log.info("Estabelecimento atualizado com sucesso: estabelecimentoId={}, usuarioId={}",
                id, usuarioId);

        EstatisticasOcorrenciaEstabelecimentoDto estatisticas =
                ocorrenciaEstabelecimentoQueryPort.obterEstatisticasPorEstabelecimento(id);
        List<ContagemItemDto> distribuicaoTipoViolencia =
                construirDistribuicaoTipoViolencia(estatisticas.distribuicaoTipoViolencia());
        return estabelecimentoMapper.toDetalheDto(estabelecimento, categoria, estatisticas, distribuicaoTipoViolencia);
    }

    @Override
    @Transactional(readOnly = true)
    public EstabelecimentoResumoDto buscarAtivoOuFalhar(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Estabelecimento informado está inativo ou não foi encontrado.");
        }
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(id)
                .filter(Estabelecimento::isAtivo)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento informado está inativo ou não foi encontrado."));
        CategoriaEstabelecimentoDto categoria = resolverCategoria(estabelecimento.getCategoriaId());
        return estabelecimentoMapper.toResumoDto(estabelecimento, categoria.nome());
    }

    @Override
    @Transactional(readOnly = true)
    public EstabelecimentoResumoDto buscarPorIdOuFalhar(UUID id) {
        Estabelecimento estabelecimento = buscarEntidadeOuFalhar(id);
        CategoriaEstabelecimentoDto categoria = resolverCategoria(estabelecimento.getCategoriaId());
        return estabelecimentoMapper.toResumoDto(estabelecimento, categoria.nome());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstabelecimentoResumoDto> buscarPorId(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return estabelecimentoRepository.findById(id)
                .map(e -> {
                    String categoriaNome = catalogoInternalFacade.buscarCategoriaPorId(e.getCategoriaId())
                            .map(CategoriaEstabelecimentoDto::nome)
                            .orElse("");
                    return estabelecimentoMapper.toResumoDto(e, categoriaNome);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstabelecimentoResumoDto> listarPorIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findAllById(ids);
        if (estabelecimentos.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> nomesCategorias = carregarNomesCategorias(estabelecimentos);
        return estabelecimentoMapper.toResumoDtoList(estabelecimentos, nomesCategorias);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeAtivoPorId(UUID id) {
        if (id == null) {
            return false;
        }
        return estabelecimentoRepository.findById(id)
                .map(Estabelecimento::isAtivo)
                .orElse(false);
    }

    private List<ContagemItemDto> construirDistribuicaoTipoViolencia(Map<Integer, Integer> contagemPorTipo) {
        if (contagemPorTipo == null || contagemPorTipo.isEmpty()) {
            return List.of();
        }
        Map<Integer, TipoViolenciaDto> tiposPorId = catalogoInternalFacade
                .listarTiposViolenciaPorIds(contagemPorTipo.keySet())
                .stream()
                .collect(Collectors.toMap(TipoViolenciaDto::id, Function.identity(), (a, _) -> a));

        return contagemPorTipo.entrySet().stream()
                .map(entry -> {
                    TipoViolenciaDto tipo = tiposPorId.get(entry.getKey());
                    if (tipo != null) {
                        return new ContagemItemDto(tipo.id(), tipo.codigo(), tipo.nome(), entry.getValue());
                    }
                    return new ContagemItemDto(entry.getKey(), "DESCONHECIDO", "Desconhecido", entry.getValue());
                })
                .sorted(Comparator.comparing(ContagemItemDto::total).reversed())
                .toList();
    }

    private void validarTermoAutocomplete(String termo) {
        if (termo == null || termo.trim().length() < 2) {
            throw new BusinessRuleException("O termo de busca do autocomplete deve conter no mínimo 2 caracteres.");
        }
    }

    private int validarLimiteAutocomplete(Integer limite) {
        int limiteEfetivo = limite != null ? limite : LIMITE_AUTOCOMPLETE_PADRAO;
        if (limiteEfetivo < 1 || limiteEfetivo > LIMITE_AUTOCOMPLETE_MAXIMO) {
            throw new BusinessRuleException("O limite do autocomplete deve estar entre 1 e 20.");
        }
        return limiteEfetivo;
    }

    private void validarCoordenadasEspaciais(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessRuleException("Latitude e longitude devem ser informadas em conjunto para busca geográfica.");
        }
    }

    private Page<Estabelecimento> buscarPaginaEstabelecimentos(
            String termo,
            Integer categoriaId,
            String cidade,
            String bairro,
            Double latitude,
            Double longitude,
            Integer raioMetros,
            PageRequest pageable
    ) {
        String termoFiltro = normalizarFiltro(termo);
        String cidadeFiltro = normalizarFiltro(cidade);
        String bairroFiltro = normalizarFiltro(bairro);

        if (latitude != null && longitude != null) {
            GeoUtils.criarPonto(latitude, longitude);
            int raioEfetivo = validarRaioBusca(raioMetros);
            return estabelecimentoRepository.listarComFiltrosEspaciais(
                    termoFiltro,
                    categoriaId,
                    cidadeFiltro,
                    bairroFiltro,
                    latitude,
                    longitude,
                    raioEfetivo,
                    pageable
            );
        }

        return estabelecimentoRepository.listarComFiltros(
                termoFiltro,
                categoriaId,
                cidadeFiltro,
                bairroFiltro,
                pageable
        );
    }

    private int validarRaioBusca(Integer raioMetros) {
        int raioEfetivo = raioMetros != null ? raioMetros : geoProperties.estabelecimento().raioPadraoMetros();
        if (raioEfetivo < geoProperties.estabelecimento().raioMinimoMetros() || raioEfetivo > geoProperties.estabelecimento().raioMaximoMetros()) {
            throw new BusinessRuleException("Raio de busca inválido: deve ser positivo e não exceder o limite máximo configurado.");
        }
        return raioEfetivo;
    }

    private EstabelecimentoPageDto construirPageDto(Page<Estabelecimento> pagina) {
        Map<Integer, String> nomesCategorias = carregarNomesCategorias(pagina.getContent());
        List<EstabelecimentoResumoDto> content =
                estabelecimentoMapper.toResumoDtoList(pagina.getContent(), nomesCategorias);

        return new EstabelecimentoPageDto(
                content,
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize()
        );
    }

    private void validarRequestCriacao(CriacaoEstabelecimentoCommand command) {
        if (command == null) {
            throw new BusinessRuleException("Dados de criação do estabelecimento são obrigatórios.");
        }
        if (command.latitude() == null || command.longitude() == null) {
            throw new BusinessRuleException("Coordenadas de latitude e longitude são obrigatórias.");
        }
    }

    private String processarCnpjCadastro(String cnpj) {
        String cnpjNormalizado = normalizarCnpj(cnpj);
        if (cnpjNormalizado != null) {
            CnpjValidator.validar(cnpjNormalizado);
            if (estabelecimentoRepository.existsByCnpj(cnpjNormalizado)) {
                throw new ConflictException("Já existe um estabelecimento cadastrado com o CNPJ informado.");
            }
        }
        return cnpjNormalizado;
    }

    private Estabelecimento criarEntidadeEstabelecimento(
            CriacaoEstabelecimentoCommand command,
            Integer categoriaId,
            String cnpjNormalizado,
            UUID usuarioId,
            OffsetDateTime agora
    ) {
        return Estabelecimento.criar(
            categoriaId,
            command.nome(),
            cnpjNormalizado,
            command.logradouro(),
            command.numero(),
            command.bairro(),
            command.cidade(),
            command.estado(),
            command.latitude(),
            command.longitude(),
            usuarioId,
            agora
        );
    }

    private void vincularGestorSeAplicavel(UUID estabelecimentoId, UUID usuarioId, OffsetDateTime agora) {
        if (authenticatedUserProvider.possuiRole(ROLE_GESTOR)) {
            EstabelecimentoGestor gestor = EstabelecimentoGestor.vincular(
                    estabelecimentoId,
                    usuarioId,
                    "GESTOR_RESPONSAVEL",
                    usuarioId,
                    agora
            );
            estabelecimentoGestorRepository.save(gestor);
            log.info("Usuário vinculado a estabelecimento com sucesso: estabelecimentoId={}, usuarioId={}",
                    estabelecimentoId, usuarioId);
        }
    }

    private void validarRequestAtualizacao(AtualizacaoEstabelecimentoCommand command) {
        if (command == null) {
            throw new BusinessRuleException("Dados de atualização do estabelecimento são obrigatórios.");
        }
    }

    private void validarPerfilGestor() {
        if (!authenticatedUserProvider.possuiRole(ROLE_GESTOR)) {
            throw new ForbiddenOperationException("Acesso restrito a usuários com perfil de gestor.");
        }
    }

    private void validarVinculoGestao(UUID estabelecimentoId, UUID usuarioId) {
        if (!estabelecimentoGestorRepository.existsByIdEstabelecimentoIdAndIdUsuarioIdAndAtivoTrue(estabelecimentoId, usuarioId)) {
            throw new ForbiddenOperationException("Usuário não possui vínculo ativo de gestão para este estabelecimento.");
        }
    }

    private void validarCnpjParaAtualizacao(String cnpjEfetivo, UUID id) {
        if (cnpjEfetivo != null) {
            CnpjValidator.validar(cnpjEfetivo);
            if (estabelecimentoRepository.existsByCnpjAndIdNot(cnpjEfetivo, id)) {
                throw new ConflictException("Já existe outro estabelecimento cadastrado com o CNPJ informado.");
            }
        }
    }

    private void aplicarAtualizacaoDadosCadastrais(
            Estabelecimento estabelecimento,
            AtualizacaoEstabelecimentoCommand command,
            CategoriaEstabelecimentoDto categoria,
            String cnpjEfetivo,
            UUID usuarioId
    ) {
        String nomeEfetivo = command.nome() != null ? command.nome() : estabelecimento.getNome();
        String logradouroEfetivo = command.logradouro() != null ? command.logradouro() : estabelecimento.getLogradouro();
        String numeroEfetivo = command.numero() != null ? command.numero() : estabelecimento.getNumero();
        String bairroEfetivo = command.bairro() != null ? command.bairro() : estabelecimento.getBairro();
        String cidadeEfetiva = command.cidade() != null ? command.cidade() : estabelecimento.getCidade();
        String estadoEfetivo = command.estado() != null ? command.estado() : estabelecimento.getEstado();

        estabelecimento.atualizarDadosCadastrais(
                nomeEfetivo,
                categoria.id(),
                cnpjEfetivo,
                logradouroEfetivo,
                numeroEfetivo,
                bairroEfetivo,
                cidadeEfetiva,
                estadoEfetivo,
                command.ativo(),
                usuarioId,
                OffsetDateTime.now(clock)
        );
    }

    private Estabelecimento buscarEntidadeOuFalhar(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Estabelecimento não encontrado.");
        }
        return estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado."));
    }

    private CategoriaEstabelecimentoDto resolverCategoria(Integer categoriaId) {
        return catalogoInternalFacade.buscarCategoriaPorId(categoriaId)
                .orElseGet(() -> new CategoriaEstabelecimentoDto(categoriaId, "OUTRO", "Outros Estabelecimentos", true));
    }

    private Map<Integer, String> carregarNomesCategorias(Collection<Estabelecimento> estabelecimentos) {
        Set<Integer> categoriaIds = estabelecimentos.stream()
                .map(Estabelecimento::getCategoriaId)
                .collect(Collectors.toSet());
        return catalogoInternalFacade.listarCategoriasPorIds(categoriaIds).stream()
                .collect(Collectors.toMap(CategoriaEstabelecimentoDto::id, CategoriaEstabelecimentoDto::nome, (a, _) -> a));
    }

    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        return cnpj.trim().toUpperCase(Locale.ROOT);
    }

    private int validarPage(Integer page) {
        int pageEfetivo = page != null ? page : PAGE_PADRAO;
        if (pageEfetivo < 0) {
            throw new BusinessRuleException("O número da página não pode ser negativo.");
        }
        return pageEfetivo;
    }

    private int validarSize(Integer size) {
        int sizeEfetivo = size != null ? size : SIZE_PADRAO;
        if (sizeEfetivo < 1 || sizeEfetivo > SIZE_MAXIMO) {
            throw new BusinessRuleException("O tamanho da página deve estar entre 1 e 100.");
        }
        return sizeEfetivo;
    }
}
