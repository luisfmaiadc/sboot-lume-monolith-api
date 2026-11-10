package br.com.lume.api.ocorrencia.service;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.estabelecimento.api.EstabelecimentoInternalFacade;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.identidade.api.IdentidadeInternalFacade;
import br.com.lume.api.ocorrencia.command.AtualizarStatusOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.RegistrarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.ValidarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.domain.StatusOcorrencia;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;
import br.com.lume.api.ocorrencia.domain.TipoValidacao;
import br.com.lume.api.ocorrencia.domain.ValidacaoOcorrencia;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaDto;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaPageDto;
import br.com.lume.api.ocorrencia.dto.OcorrenciaDetalheDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.repository.OcorrenciaRepository;
import br.com.lume.api.ocorrencia.repository.ValidacaoOcorrenciaRepository;
import br.com.lume.api.shared.event.ReputacaoEstabelecimentoRecalculadaEvent;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.security.AuthenticatedUserProvider;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    public static final int LIMITE_OCORRENCIAS_24H = 5;
    private static final int PAGE_PADRAO = 0;
    private static final int SIZE_PADRAO = 20;
    private static final int SIZE_MAXIMO = 100;

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ValidacaoOcorrenciaRepository validacaoOcorrenciaRepository;
    private final CatalogoInternalFacade catalogoInternalFacade;
    private final EstabelecimentoInternalFacade estabelecimentoInternalFacade;
    private final IdentidadeInternalFacade identidadeInternalFacade;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final ZoneId zonaNegocio;
    private final OcorrenciaMapper ocorrenciaMapper;

    @Transactional
    public OcorrenciaDetalheDto registrarOcorrencia(RegistrarOcorrenciaCommand command) {
        validarCommandRegistro(command);

        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        identidadeInternalFacade.adquirirLockParaUsuario(usuarioId);
        validarLimiteDiarioOcorrencias(usuarioId);
        TipoViolenciaDto tipoViolencia = catalogoInternalFacade.buscarTipoViolenciaAtivoOuFalhar(command.tipoViolenciaId());
        MarcadorVulnerabilidadeDto marcador = carregarMarcadorOpcional(command.marcadorVulnerabilidadeId());
        EstabelecimentoResumoDto estabelecimento = carregarEstabelecimentoOpcional(command.estabelecimentoId());
        DadosEspaciaisOcorrencia dadosEspaciais = resolverDadosEspaciais(command, estabelecimento);

        Ocorrencia ocorrencia = criarEntidadeOcorrencia(command, tipoViolencia, marcador, estabelecimento, dadosEspaciais, usuarioId);
        ocorrenciaRepository.save(ocorrencia);
        log.info("Ocorrência registrada com sucesso: ocorrenciaId={}, tipoAmbiente={}, estabelecimentoId={}",
                ocorrencia.getId(), ocorrencia.getTipoAmbiente(), ocorrencia.getEstabelecimentoId());

        notificarReputacaoSeNecessario(ocorrencia.getEstabelecimentoId());

        String estabelecimentoNome = estabelecimento != null ? estabelecimento.nome() : null;
        return ocorrenciaMapper.toDetalheDto(ocorrencia, tipoViolencia, marcador, estabelecimentoNome, zonaNegocio);
    }

    @Transactional(readOnly = true)
    public OcorrenciaDetalheDto obterOcorrenciaPorId(UUID id) {
        Ocorrencia ocorrencia = buscarPublicadaOuFalhar(id);
        return construirDetalheDto(ocorrencia);
    }

    @Transactional(readOnly = true)
    public MinhaOcorrenciaPageDto listarMinhasOcorrencias(Integer page, Integer size) {
        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        PageRequest pageable = PageRequest.of(validarPage(page), validarSize(size));
        Page<Ocorrencia> pagina = ocorrenciaRepository.findByCriadoPorOrderByCriadoEmDesc(usuarioId, pageable);

        return construirMinhaOcorrenciaPageDto(pagina);
    }

    @Transactional
    public void excluirOcorrencia(UUID id) {
        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        Ocorrencia ocorrencia = buscarPorIdOuFalhar(id);

        ocorrencia.excluirLogicamente(usuarioId, OffsetDateTime.now(clock));
        ocorrenciaRepository.save(ocorrencia);
        log.info("Ocorrência excluída com sucesso: ocorrenciaId={}", id);

        notificarReputacaoSeNecessario(ocorrencia.getEstabelecimentoId());
    }

    @Transactional
    public OcorrenciaDetalheDto atualizarStatusOcorrencia(UUID id, AtualizarStatusOcorrenciaCommand command) {
        validarCommandStatus(command);

        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        Ocorrencia ocorrencia = buscarPorIdOuFalhar(id);

        aplicarTransicaoStatus(ocorrencia, command.status(), usuarioId, OffsetDateTime.now(clock));
        ocorrenciaRepository.save(ocorrencia);
        log.info("Status da ocorrência atualizado: ocorrenciaId={}, novoStatus={}", id, command.status());

        notificarReputacaoSeNecessario(ocorrencia.getEstabelecimentoId());

        return construirDetalheDto(ocorrencia);
    }

    @Transactional
    public void validarOcorrencia(UUID ocorrenciaId, ValidarOcorrenciaCommand command) {
        validarCommandValidacao(command);

        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        Ocorrencia ocorrencia = buscarPublicadaOuFalhar(ocorrenciaId);
        OffsetDateTime agora = OffsetDateTime.now(clock);

        ValidacaoOcorrencia validacao = processarRegistroValidacao(ocorrencia, usuarioId, command.tipoValidacao(), agora);
        validacaoOcorrenciaRepository.save(validacao);

        ocorrenciaRepository.incrementarTotalValidacoes(ocorrenciaId, agora);
        log.info("Validação comunitária registrada com sucesso: ocorrenciaId={}", ocorrenciaId);

        notificarReputacaoSeNecessario(ocorrencia.getEstabelecimentoId());
    }

    @Transactional
    public void cancelarValidacaoOcorrencia(UUID ocorrenciaId) {
        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        Ocorrencia ocorrencia = buscarPublicadaOuFalhar(ocorrenciaId);
        ValidacaoOcorrencia validacao = buscarValidacaoAtivaOuFalhar(ocorrenciaId, usuarioId);

        OffsetDateTime agora = OffsetDateTime.now(clock);
        validacao.inativar(usuarioId, agora);
        validacaoOcorrenciaRepository.save(validacao);

        ocorrenciaRepository.decrementarTotalValidacoes(ocorrenciaId, agora);
        log.info("Validação comunitária cancelada com sucesso: ocorrenciaId={}", ocorrenciaId);

        notificarReputacaoSeNecessario(ocorrencia.getEstabelecimentoId());
    }

    // --- Métodos Auxiliares de Validação e Consulta ---

    private void validarCommandRegistro(RegistrarOcorrenciaCommand command) {
        if (command == null) {
            throw new BusinessRuleException("Dados de registro da ocorrência são obrigatórios.");
        }
    }

    private void validarLimiteDiarioOcorrencias(UUID usuarioId) {
        OffsetDateTime limite24h = OffsetDateTime.now(clock).minusHours(24);
        long ocorrenciasRecentes = ocorrenciaRepository.countByCriadoPorAndCriadoEmAfter(usuarioId, limite24h);
        if (ocorrenciasRecentes >= LIMITE_OCORRENCIAS_24H) {
            throw new BusinessRuleException("Limite de 5 ocorrências registradas nas últimas 24 horas atingido.");
        }
    }

    private void validarCommandStatus(AtualizarStatusOcorrenciaCommand command) {
        if (command == null || command.status() == null) {
            throw new BusinessRuleException("Novo status da ocorrência é obrigatório.");
        }
    }

    private void validarCommandValidacao(ValidarOcorrenciaCommand command) {
        if (command == null || command.tipoValidacao() == null) {
            throw new BusinessRuleException("Tipo de validação comunitária é obrigatório.");
        }
    }

    private Ocorrencia buscarPorIdOuFalhar(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Ocorrência não encontrada.");
        }
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência não encontrada."));
    }

    private Ocorrencia buscarPublicadaOuFalhar(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Ocorrência não encontrada.");
        }
        return ocorrenciaRepository.findByIdAndStatus(id, StatusOcorrencia.PUBLICADA)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência não encontrada."));
    }

    private ValidacaoOcorrencia buscarValidacaoAtivaOuFalhar(UUID ocorrenciaId, UUID usuarioId) {
        return validacaoOcorrenciaRepository
                .findByOcorrenciaIdAndUsuarioIdAndAtivoTrue(ocorrenciaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Validação comunitária não encontrada para esta ocorrência."));
    }

    private MarcadorVulnerabilidadeDto carregarMarcadorOpcional(Integer marcadorId) {
        return marcadorId != null ? catalogoInternalFacade.buscarMarcadorAtivoOuFalhar(marcadorId) : null;
    }

    private EstabelecimentoResumoDto carregarEstabelecimentoOpcional(UUID estabelecimentoId) {
        return estabelecimentoId != null ? estabelecimentoInternalFacade.buscarAtivoOuFalhar(estabelecimentoId) : null;
    }

    // --- Métodos Auxiliares de Espaço e Montagem ---

    private DadosEspaciaisOcorrencia resolverDadosEspaciais(
            RegistrarOcorrenciaCommand command,
            EstabelecimentoResumoDto estabelecimento
    ) {
        if (estabelecimento != null) {
            return resolverDadosDeEstabelecimento(command, estabelecimento);
        }
        return resolverDadosAvulsos(command);
    }

    private DadosEspaciaisOcorrencia resolverDadosDeEstabelecimento(
            RegistrarOcorrenciaCommand command,
            EstabelecimentoResumoDto estabelecimento
    ) {
        if (command.tipoAmbiente() != null && command.tipoAmbiente() != TipoAmbiente.ESTABELECIMENTO) {
            throw new BusinessRuleException("Ocorrências vinculadas a estabelecimento devem ter tipo de ambiente ESTABELECIMENTO.");
        }
        return new DadosEspaciaisOcorrencia(
                estabelecimento.latitude(),
                estabelecimento.longitude(),
                estabelecimento.logradouro(),
                estabelecimento.numero(),
                estabelecimento.bairro(),
                estabelecimento.cidade(),
                estabelecimento.estado(),
                TipoAmbiente.ESTABELECIMENTO
        );
    }

    private DadosEspaciaisOcorrencia resolverDadosAvulsos(RegistrarOcorrenciaCommand command) {
        if (command.latitude() == null || command.longitude() == null) {
            throw new BusinessRuleException("Coordenadas de latitude e longitude são obrigatórias.");
        }
        if (command.tipoAmbiente() == null) {
            throw new BusinessRuleException("Tipo de ambiente da ocorrência é obrigatório.");
        }
        if (command.tipoAmbiente() == TipoAmbiente.ESTABELECIMENTO) {
            throw new BusinessRuleException("Para tipo de ambiente ESTABELECIMENTO, o ID do estabelecimento deve ser informado.");
        }
        return new DadosEspaciaisOcorrencia(
                command.latitude(),
                command.longitude(),
                command.logradouro(),
                command.numero(),
                command.bairro(),
                command.cidade(),
                command.estado(),
                command.tipoAmbiente()
        );
    }

    private Ocorrencia criarEntidadeOcorrencia(
            RegistrarOcorrenciaCommand command,
            TipoViolenciaDto tipoViolencia,
            MarcadorVulnerabilidadeDto marcador,
            EstabelecimentoResumoDto estabelecimento,
            DadosEspaciaisOcorrencia dadosEspaciais,
            UUID usuarioId
    ) {
        return Ocorrencia.registrar(
                estabelecimento != null ? estabelecimento.id() : null,
                dadosEspaciais.latitude(),
                dadosEspaciais.longitude(),
                dadosEspaciais.logradouro(),
                dadosEspaciais.numero(),
                dadosEspaciais.bairro(),
                dadosEspaciais.cidade(),
                dadosEspaciais.estado(),
                command.dataOcorrencia(),
                command.faixaHorario(),
                tipoViolencia.id(),
                marcador != null ? marcador.id() : null,
                dadosEspaciais.tipoAmbiente(),
                usuarioId,
                OffsetDateTime.now(clock),
                zonaNegocio
        );
    }

    private OcorrenciaDetalheDto construirDetalheDto(Ocorrencia ocorrencia) {
        TipoViolenciaDto tipoViolencia = resolverTipoViolencia(ocorrencia.getTipoViolenciaId());
        MarcadorVulnerabilidadeDto marcador = resolverMarcador(ocorrencia.getMarcadorVulnerabilidadeId());
        String estabelecimentoNome = resolverNomeEstabelecimento(ocorrencia.getEstabelecimentoId());
        return ocorrenciaMapper.toDetalheDto(ocorrencia, tipoViolencia, marcador, estabelecimentoNome, zonaNegocio);
    }

    private MinhaOcorrenciaPageDto construirMinhaOcorrenciaPageDto(Page<Ocorrencia> pagina) {
        List<Ocorrencia> registros = pagina.getContent();
        Map<Integer, String> nomesViolencia = carregarNomesTiposViolencia(registros);
        Map<UUID, String> nomesEstabelecimentos = carregarNomesEstabelecimentos(registros);

        List<MinhaOcorrenciaDto> content = ocorrenciaMapper.toMinhaOcorrenciaDtoList(
                registros,
                nomesViolencia,
                nomesEstabelecimentos,
                OffsetDateTime.now(clock)
        );

        return new MinhaOcorrenciaPageDto(
                content,
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize()
        );
    }

    // --- Métodos Auxiliares de Moderação e Validação ---

    private void aplicarTransicaoStatus(
            Ocorrencia ocorrencia,
            StatusOcorrencia novoStatus,
            UUID usuarioId,
            OffsetDateTime dataHora
    ) {
        if (authenticatedUserProvider.possuiRole("ROLE_ADMIN")) {
            ocorrencia.atualizarStatusPorAdmin(novoStatus, usuarioId, dataHora);
        } else if (authenticatedUserProvider.possuiRole("ROLE_MODERADOR")) {
            ocorrencia.atualizarStatusPorModerador(novoStatus, usuarioId, dataHora);
        } else {
            throw new AccessDeniedException("Acesso negado: apenas moderadores e administradores podem atualizar o status da ocorrência.");
        }
    }

    private ValidacaoOcorrencia processarRegistroValidacao(
            Ocorrencia ocorrencia,
            UUID usuarioId,
            TipoValidacao tipoValidacao,
            OffsetDateTime agora
    ) {
        return validacaoOcorrenciaRepository.findByOcorrenciaIdAndUsuarioId(ocorrencia.getId(), usuarioId)
                .map(existente -> reativarValidacao(ocorrencia, existente, tipoValidacao, usuarioId, agora))
                .orElseGet(() -> ValidacaoOcorrencia.criar(ocorrencia, usuarioId, tipoValidacao, agora));
    }

    private ValidacaoOcorrencia reativarValidacao(
            Ocorrencia ocorrencia,
            ValidacaoOcorrencia validacao,
            TipoValidacao tipoValidacao,
            UUID usuarioId,
            OffsetDateTime agora
    ) {
        if (validacao.isAtivo()) {
            throw new ConflictException("O usuário já validou esta ocorrência anteriormente.");
        }
        if (!ocorrencia.permiteValidacao(agora)) {
            throw new BusinessRuleException("Ocorrências só podem receber validações comunitárias em até 30 dias após seu registro.");
        }
        validacao.reativar(tipoValidacao, usuarioId, agora);
        return validacao;
    }

    private void notificarReputacaoSeNecessario(UUID estabelecimentoId) {
        if (estabelecimentoId != null) {
            eventPublisher.publishEvent(new ReputacaoEstabelecimentoRecalculadaEvent(
                    estabelecimentoId,
                    Instant.now(clock)
            ));
            log.info("Evento ReputacaoEstabelecimentoRecalculadaEvent publicado: estabelecimentoId={}", estabelecimentoId);
        }
    }

    // --- Métodos Auxiliares de Catálogo e Estabelecimento ---

    private TipoViolenciaDto resolverTipoViolencia(Integer tipoViolenciaId) {
        return catalogoInternalFacade.buscarTipoViolenciaPorId(tipoViolenciaId)
                .orElseGet(() -> new TipoViolenciaDto(tipoViolenciaId, "OUTRO", "Outro Tipo de Violência", 3, null, true));
    }

    private MarcadorVulnerabilidadeDto resolverMarcador(Integer marcadorId) {
        if (marcadorId == null) {
            return null;
        }
        return catalogoInternalFacade.buscarMarcadorPorId(marcadorId).orElse(null);
    }

    private String resolverNomeEstabelecimento(UUID estabelecimentoId) {
        if (estabelecimentoId == null) {
            return null;
        }
        return estabelecimentoInternalFacade.buscarPorId(estabelecimentoId)
                .map(EstabelecimentoResumoDto::nome)
                .orElse(null);
    }

    private Map<Integer, String> carregarNomesTiposViolencia(Collection<Ocorrencia> ocorrencias) {
        if (ocorrencias.isEmpty()) {
            return Map.of();
        }
        Set<Integer> ids = ocorrencias.stream()
                .map(Ocorrencia::getTipoViolenciaId)
                .collect(Collectors.toSet());
        return catalogoInternalFacade.listarTiposViolenciaPorIds(ids).stream()
                .collect(Collectors.toMap(TipoViolenciaDto::id, TipoViolenciaDto::nome, (a, _) -> a));
    }

    private Map<UUID, String> carregarNomesEstabelecimentos(Collection<Ocorrencia> ocorrencias) {
        Set<UUID> estabelecimentoIds = ocorrencias.stream()
                .map(Ocorrencia::getEstabelecimentoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (estabelecimentoIds.isEmpty()) {
            return Map.of();
        }
        return estabelecimentoInternalFacade.listarPorIds(estabelecimentoIds).stream()
                .collect(Collectors.toMap(EstabelecimentoResumoDto::id, EstabelecimentoResumoDto::nome, (a, _) -> a));
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

    private record DadosEspaciaisOcorrencia(
            Double latitude,
            Double longitude,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String estado,
            TipoAmbiente tipoAmbiente
    ) {}
}
