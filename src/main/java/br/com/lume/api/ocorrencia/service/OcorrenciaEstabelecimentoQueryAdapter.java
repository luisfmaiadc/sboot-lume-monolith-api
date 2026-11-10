package br.com.lume.api.ocorrencia.service;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.domain.StatusOcorrencia;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.repository.OcorrenciaRepository;
import br.com.lume.api.shared.port.EstatisticasOcorrenciaEstabelecimentoDto;
import br.com.lume.api.shared.port.OcorrenciaEstabelecimentoQueryPort;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPortDto;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OcorrenciaEstabelecimentoQueryAdapter implements OcorrenciaEstabelecimentoQueryPort {

    private static final BigDecimal SCORE_PADRAO_ESTABELECIMENTO = new BigDecimal("5.00");
    private static final int PESO_GRAVIDADE_DEFAULT = 1;

    private final OcorrenciaRepository ocorrenciaRepository;
    private final CatalogoInternalFacade catalogoInternalFacade;
    private final CalculadoraReputacaoRiscoService calculadoraReputacaoRiscoService;
    private final OcorrenciaMapper ocorrenciaMapper;
    private final Clock clock;
    private final ZoneId zonaNegocio;

    @Override
    @Transactional(readOnly = true)
    public OcorrenciaResumoPagePortDto listarPorEstabelecimento(
            UUID estabelecimentoId,
            Integer tipoViolenciaId,
            Integer marcadorId,
            String faixaHorario,
            int page,
            int size
    ) {
        Page<Ocorrencia> pagina = ocorrenciaRepository.listarPublicadasPorEstabelecimento(
                estabelecimentoId,
                tipoViolenciaId,
                marcadorId,
                toDomainFaixaHorario(faixaHorario),
                PageRequest.of(page, size)
        );

        if (pagina.isEmpty()) {
            return criarPageResult(pagina, List.of());
        }

        List<Ocorrencia> registros = pagina.getContent();
        Map<Integer, TipoViolenciaDto> tipos = carregarTiposViolencia(registros);
        Map<Integer, MarcadorVulnerabilidadeDto> marcadores = carregarMarcadores(registros);

        List<OcorrenciaResumoPortDto> content = ocorrenciaMapper.toResumoPortDtoList(registros, tipos, marcadores);
        return criarPageResult(pagina, content);
    }

    @Override
    @Transactional(readOnly = true)
    public EstatisticasOcorrenciaEstabelecimentoDto obterEstatisticasPorEstabelecimento(UUID estabelecimentoId) {
        if (estabelecimentoId == null) {
            return new EstatisticasOcorrenciaEstabelecimentoDto(0, 0, 0, 0, 0, Map.of());
        }

        List<Ocorrencia> ocorrencias = ocorrenciaRepository
                .findByEstabelecimentoIdAndStatusOrderByDataOcorrenciaDescCriadoEmDesc(
                        estabelecimentoId,
                        StatusOcorrencia.PUBLICADA
                );

        return construirEstatisticas(ocorrencias);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularScoreReputacao(UUID estabelecimentoId) {
        if (estabelecimentoId == null) {
            return SCORE_PADRAO_ESTABELECIMENTO;
        }

        List<Ocorrencia> ocorrencias = ocorrenciaRepository
                .findByEstabelecimentoIdAndStatusOrderByDataOcorrenciaDescCriadoEmDesc(
                        estabelecimentoId,
                        StatusOcorrencia.PUBLICADA
                );

        if (ocorrencias.isEmpty()) {
            return SCORE_PADRAO_ESTABELECIMENTO;
        }

        Map<Integer, Integer> pesosPorTipo = carregarPesosPorTipo(ocorrencias);
        LocalDate hoje = LocalDate.now(clock.withZone(zonaNegocio));
        BigDecimal somaImpactos = calcularSomaImpactos(ocorrencias, pesosPorTipo, hoje);

        return calculadoraReputacaoRiscoService.calcularScoreEstabelecimento(somaImpactos);
    }

    // --- Métodos Auxiliares: Listagem de Ocorrências ---

    private FaixaHorario toDomainFaixaHorario(String faixaHorario) {
        return faixaHorario != null && !faixaHorario.isBlank()
                ? FaixaHorario.valueOf(faixaHorario)
                : null;
    }

    private Map<Integer, TipoViolenciaDto> carregarTiposViolencia(List<Ocorrencia> ocorrencias) {
        Set<Integer> tipoIds = ocorrencias.stream()
                .map(Ocorrencia::getTipoViolenciaId)
                .collect(Collectors.toSet());

        return catalogoInternalFacade.listarTiposViolenciaPorIds(tipoIds).stream()
                .collect(Collectors.toMap(TipoViolenciaDto::id, Function.identity(), (a, _) -> a));
    }

    private Map<Integer, MarcadorVulnerabilidadeDto> carregarMarcadores(List<Ocorrencia> ocorrencias) {
        Set<Integer> marcadorIds = ocorrencias.stream()
                .map(Ocorrencia::getMarcadorVulnerabilidadeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (marcadorIds.isEmpty()) {
            return Map.of();
        }

        return catalogoInternalFacade.listarMarcadoresPorIds(marcadorIds).stream()
                .collect(Collectors.toMap(MarcadorVulnerabilidadeDto::id, Function.identity(), (a, _) -> a));
    }

    private OcorrenciaResumoPagePortDto criarPageResult(Page<Ocorrencia> pagina, List<OcorrenciaResumoPortDto> content) {
        return new OcorrenciaResumoPagePortDto(
                content,
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize()
        );
    }

    // --- Métodos Auxiliares: Estatísticas de Ocorrências ---

    private EstatisticasOcorrenciaEstabelecimentoDto construirEstatisticas(List<Ocorrencia> ocorrencias) {
        if (ocorrencias.isEmpty()) {
            return new EstatisticasOcorrenciaEstabelecimentoDto(0, 0, 0, 0, 0, Map.of());
        }

        int madrugada = contarPorFaixa(ocorrencias, FaixaHorario.MADRUGADA);
        int manha = contarPorFaixa(ocorrencias, FaixaHorario.MANHA);
        int tarde = contarPorFaixa(ocorrencias, FaixaHorario.TARDE);
        int noite = contarPorFaixa(ocorrencias, FaixaHorario.NOITE);
        Map<Integer, Integer> distribuicaoTipos = calcularDistribuicaoTipos(ocorrencias);

        return new EstatisticasOcorrenciaEstabelecimentoDto(
                ocorrencias.size(),
                madrugada,
                manha,
                tarde,
                noite,
                distribuicaoTipos
        );
    }

    private int contarPorFaixa(List<Ocorrencia> ocorrencias, FaixaHorario faixa) {
        return (int) ocorrencias.stream()
                .filter(o -> o.getFaixaHorario() == faixa)
                .count();
    }

    private Map<Integer, Integer> calcularDistribuicaoTipos(List<Ocorrencia> ocorrencias) {
        return ocorrencias.stream()
                .map(Ocorrencia::getTipoViolenciaId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    // --- Métodos Auxiliares: Cálculo de Score e Reputação ---

    private Map<Integer, Integer> carregarPesosPorTipo(List<Ocorrencia> ocorrencias) {
        Set<Integer> tipoIds = ocorrencias.stream()
                .map(Ocorrencia::getTipoViolenciaId)
                .collect(Collectors.toSet());

        return catalogoInternalFacade.listarTiposViolenciaPorIds(tipoIds).stream()
                .collect(Collectors.toMap(TipoViolenciaDto::id, TipoViolenciaDto::pesoGravidade, (a, _) -> a));
    }

    private BigDecimal calcularSomaImpactos(
            List<Ocorrencia> ocorrencias,
            Map<Integer, Integer> pesosPorTipo,
            LocalDate dataReferencia
    ) {
        return ocorrencias.stream()
                .map(o -> calcularImpactoIndividual(o, pesosPorTipo, dataReferencia))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularImpactoIndividual(
            Ocorrencia ocorrencia,
            Map<Integer, Integer> pesosPorTipo,
            LocalDate dataReferencia
    ) {
        int peso = pesosPorTipo.getOrDefault(ocorrencia.getTipoViolenciaId(), PESO_GRAVIDADE_DEFAULT);
        return calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(
                peso,
                ocorrencia.getDataOcorrencia(),
                ocorrencia.getTotalValidacoes(),
                dataReferencia
        );
    }
}
