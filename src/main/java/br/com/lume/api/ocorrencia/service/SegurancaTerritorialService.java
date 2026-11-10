package br.com.lume.api.ocorrencia.service;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.dto.DiagnosticoSegurancaDto;
import br.com.lume.api.ocorrencia.dto.PontoMapaDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.repository.AgrupamentoDiagnosticoProjection;
import br.com.lume.api.ocorrencia.repository.OcorrenciaRepository;
import br.com.lume.api.shared.config.GeoProperties;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.port.ContagemItemDto;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import br.com.lume.api.shared.util.GeoUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SegurancaTerritorialService {

    public static final int LIMIAR_K_ANONIMIZACAO = 3;
    private static final BigDecimal PASSO_DISCRETIZACAO_SCORE = new BigDecimal("0.50");
    private static final int DIAS_PADRAO_MAPA = 90;
    private static final int DIAS_JANELA_DIAGNOSTICO = 365;

    private static final Comparator<ContagemItemDto> COMPARADOR_CONTAGEM_ITEM = Comparator
            .comparingInt(ContagemItemDto::total)
            .reversed()
            .thenComparingInt(ContagemItemDto::id);

    private final OcorrenciaRepository ocorrenciaRepository;
    private final CatalogoInternalFacade catalogoInternalFacade;
    private final CalculadoraReputacaoRiscoService calculadoraReputacaoRiscoService;
    private final GeoProperties geoProperties;
    private final Clock clock;
    private final ZoneId zonaNegocio;
    private final OcorrenciaMapper ocorrenciaMapper;

    @Transactional(readOnly = true)
    public DiagnosticoSegurancaDto obterDiagnosticoRaio(
            Double latitude,
            Double longitude,
            Integer raioMetros
    ) {
        validarCoordenadasObrigatorias(latitude, longitude);
        int raioEfetivo = validarRaioDiagnostico(raioMetros);

        LocalDate hoje = LocalDate.now(clock.withZone(zonaNegocio));
        LocalDate dataMinima = hoje.minusDays(DIAS_JANELA_DIAGNOSTICO);

        List<AgrupamentoDiagnosticoProjection> agrupamentos = ocorrenciaRepository.agruparPublicadasNoRaio(
                latitude,
                longitude,
                raioEfetivo,
                dataMinima
        );

        Map<Integer, TipoViolenciaDto> tiposPorId = carregarTiposViolenciaDeAgrupamentos(agrupamentos);
        Map<Integer, MarcadorVulnerabilidadeDto> marcadoresPorId = carregarMarcadoresDeAgrupamentos(agrupamentos);

        BigDecimal somaImpactos = calcularSomaImpactos(agrupamentos, tiposPorId, hoje);
        BigDecimal scoreRiscoExato = calculadoraReputacaoRiscoService.calcularScoreRiscoViaPublica(somaImpactos);
        BigDecimal scoreRisco = discretizarScoreRisco(scoreRiscoExato);
        NivelRisco nivelRisco = calculadoraReputacaoRiscoService.classificarNivelRisco(scoreRisco);

        int totalOcorrencias = agrupamentos.stream()
                .mapToInt(AgrupamentoDiagnosticoProjection::getQuantidade)
                .sum();
        DistribuicaoTurnoDto distribuicaoTurno = calcularDistribuicaoTurno(agrupamentos, totalOcorrencias);
        List<ContagemItemDto> distribuicaoViolencia = calcularDistribuicaoViolencia(agrupamentos, tiposPorId);
        List<ContagemItemDto> distribuicaoMarcadores = calcularDistribuicaoMarcadores(agrupamentos, marcadoresPorId);

        return new DiagnosticoSegurancaDto(
                latitude,
                longitude,
                raioEfetivo,
                nivelRisco,
                scoreRisco.doubleValue(),
                totalOcorrencias,
                distribuicaoTurno,
                distribuicaoViolencia,
                distribuicaoMarcadores
        );
    }

    private BigDecimal discretizarScoreRisco(BigDecimal scoreExato) {
        if (scoreExato == null || scoreExato.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scoreExato
                .divide(PASSO_DISCRETIZACAO_SCORE, 0, RoundingMode.HALF_UP)
                .multiply(PASSO_DISCRETIZACAO_SCORE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularSomaImpactos(
            List<AgrupamentoDiagnosticoProjection> agrupamentos,
            Map<Integer, TipoViolenciaDto> tiposPorId,
            LocalDate hoje
    ) {
        BigDecimal somaImpactos = BigDecimal.ZERO;
        for (AgrupamentoDiagnosticoProjection row : agrupamentos) {
            TipoViolenciaDto tv = tiposPorId.get(row.getTipoViolenciaId());
            int pesoGravidade = tv != null ? tv.pesoGravidade() : 1;

            BigDecimal impactoUnitario = calculadoraReputacaoRiscoService.calcularImpactoOcorrencia(
                    pesoGravidade,
                    row.getDataOcorrencia(),
                    row.getValidacoesComputadas(),
                    hoje
            );
            BigDecimal impactoGrupo = impactoUnitario.multiply(BigDecimal.valueOf(row.getQuantidade()));
            somaImpactos = somaImpactos.add(impactoGrupo);
        }
        return somaImpactos;
    }

    private DistribuicaoTurnoDto calcularDistribuicaoTurno(
            List<AgrupamentoDiagnosticoProjection> agrupamentos,
            int totalOcorrencias
    ) {
        if (totalOcorrencias < LIMIAR_K_ANONIMIZACAO) {
            return new DistribuicaoTurnoDto(0, 0, 0, 0);
        }

        int madrugada = 0;
        int manha = 0;
        int tarde = 0;
        int noite = 0;

        for (AgrupamentoDiagnosticoProjection row : agrupamentos) {
            if (row.getFaixaHorario() != null) {
                switch (row.getFaixaHorario()) {
                    case "MADRUGADA" -> madrugada += row.getQuantidade();
                    case "MANHA" -> manha += row.getQuantidade();
                    case "TARDE" -> tarde += row.getQuantidade();
                    case "NOITE" -> noite += row.getQuantidade();
                    default -> {
                    }
                }
            }
        }

        return new DistribuicaoTurnoDto(
                suprimirAbaixoDeK(madrugada),
                suprimirAbaixoDeK(manha),
                suprimirAbaixoDeK(tarde),
                suprimirAbaixoDeK(noite)
        );
    }

    private int suprimirAbaixoDeK(int valor) {
        return valor >= LIMIAR_K_ANONIMIZACAO ? valor : 0;
    }

    private List<ContagemItemDto> calcularDistribuicaoViolencia(
            List<AgrupamentoDiagnosticoProjection> agrupamentos,
            Map<Integer, TipoViolenciaDto> tiposPorId
    ) {
        Map<Integer, Integer> contagem = agruparContagem(
                agrupamentos,
                AgrupamentoDiagnosticoProjection::getTipoViolenciaId
        );
        List<ContagemItemDto> itens = new ArrayList<>();
        int somaOutros = 0;

        for (Map.Entry<Integer, Integer> entry : contagem.entrySet()) {
            int total = entry.getValue();
            if (total < LIMIAR_K_ANONIMIZACAO) {
                somaOutros += total;
            } else {
                Integer id = entry.getKey();
                TipoViolenciaDto tv = tiposPorId.get(id);
                itens.add(new ContagemItemDto(
                        id,
                        tv != null ? tv.codigo() : "OUTRO",
                        tv != null ? tv.nome() : "Outro Tipo de Violência",
                        total
                ));
            }
        }

        if (somaOutros >= LIMIAR_K_ANONIMIZACAO) {
            itens.add(new ContagemItemDto(0, "OUTROS", "Outros Tipos de Violência", somaOutros));
        }

        itens.sort(COMPARADOR_CONTAGEM_ITEM);
        return List.copyOf(itens);
    }

    private List<ContagemItemDto> calcularDistribuicaoMarcadores(
            List<AgrupamentoDiagnosticoProjection> agrupamentos,
            Map<Integer, MarcadorVulnerabilidadeDto> marcadoresPorId
    ) {
        Map<Integer, Integer> contagem = agruparContagem(
                agrupamentos,
                AgrupamentoDiagnosticoProjection::getMarcadorVulnerabilidadeId
        );
        List<ContagemItemDto> itens = new ArrayList<>();
        int somaOutros = 0;

        for (Map.Entry<Integer, Integer> entry : contagem.entrySet()) {
            int total = entry.getValue();
            if (total < LIMIAR_K_ANONIMIZACAO) {
                somaOutros += total;
            } else {
                Integer id = entry.getKey();
                MarcadorVulnerabilidadeDto m = marcadoresPorId.get(id);
                itens.add(new ContagemItemDto(
                        id,
                        m != null ? m.codigo() : "OUTRO",
                        m != null ? m.nome() : "Outro Marcador",
                        total
                ));
            }
        }

        if (somaOutros >= LIMIAR_K_ANONIMIZACAO) {
            itens.add(new ContagemItemDto(0, "OUTROS", "Outros Marcadores", somaOutros));
        }

        itens.sort(COMPARADOR_CONTAGEM_ITEM);
        return List.copyOf(itens);
    }

    private Map<Integer, Integer> agruparContagem(
            List<AgrupamentoDiagnosticoProjection> agrupamentos,
            Function<AgrupamentoDiagnosticoProjection, Integer> idExtractor
    ) {
        Map<Integer, Integer> contagem = new HashMap<>();
        for (AgrupamentoDiagnosticoProjection row : agrupamentos) {
            Integer id = idExtractor.apply(row);
            if (id != null) {
                contagem.merge(id, row.getQuantidade(), Integer::sum);
            }
        }
        return contagem;
    }

    @Transactional(readOnly = true)
    public List<PontoMapaDto> listarOcorrenciasParaMapa(
            Double latitude,
            Double longitude,
            Integer raioMetros,
            Integer dias,
            Integer tipoViolenciaId,
            Integer marcadorId,
            FaixaHorario faixaHorario
    ) {
        validarCoordenadasObrigatorias(latitude, longitude);
        int raioEfetivo = validarRaioMapa(raioMetros);
        int diasEfetivo = validarDiasMapa(dias);
        int limite = geoProperties.mapa().limiteMaximoPontos();

        LocalDate dataMinima = LocalDate.now(clock.withZone(zonaNegocio)).minusDays(diasEfetivo);
        List<Ocorrencia> ocorrencias = ocorrenciaRepository.listarPublicadasParaMapa(
                latitude,
                longitude,
                raioEfetivo,
                dataMinima,
                tipoViolenciaId,
                marcadorId,
                faixaHorario != null ? faixaHorario.name() : null,
                limite
        );

        if (ocorrencias.isEmpty()) {
            return List.of();
        }

        Map<Integer, TipoViolenciaDto> tiposPorId = carregarTiposViolencia(ocorrencias);

        return ocorrenciaMapper.toPontoMapaDtoList(ocorrencias, tiposPorId);
    }

    private void validarCoordenadasObrigatorias(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessRuleException("Coordenadas de latitude e longitude são obrigatórias.");
        }
        GeoUtils.criarPonto(latitude, longitude);
    }

    private int validarRaioDiagnostico(Integer raioMetros) {
        int raioEfetivo = raioMetros != null ? raioMetros : geoProperties.diagnostico().raioPadraoMetros();
        if (raioEfetivo < geoProperties.diagnostico().raioMinimoMetros() || raioEfetivo > geoProperties.diagnostico().raioMaximoMetros()) {
            throw new BusinessRuleException("O raio espacial de diagnóstico deve estar entre "
                    + geoProperties.diagnostico().raioMinimoMetros() + " e "
                    + geoProperties.diagnostico().raioMaximoMetros() + " metros.");
        }
        return raioEfetivo;
    }

    private int validarRaioMapa(Integer raioMetros) {
        int raioEfetivo = raioMetros != null ? raioMetros : geoProperties.mapa().raioPadraoMetros();
        if (raioEfetivo < geoProperties.mapa().raioMinimoMetros() || raioEfetivo > geoProperties.mapa().raioMaximoMetros()) {
            throw new BusinessRuleException("O raio espacial do mapa deve estar entre "
                    + geoProperties.mapa().raioMinimoMetros() + " e "
                    + geoProperties.mapa().raioMaximoMetros() + " metros.");
        }
        return raioEfetivo;
    }

    private int validarDiasMapa(Integer dias) {
        int diasEfetivo = dias != null ? dias : DIAS_PADRAO_MAPA;
        if (diasEfetivo < 1 || diasEfetivo > geoProperties.mapa().diasMaximo()) {
            throw new BusinessRuleException("A janela retroativa de dias deve estar entre 1 e "
                    + geoProperties.mapa().diasMaximo() + ".");
        }
        return diasEfetivo;
    }

    private Map<Integer, TipoViolenciaDto> carregarTiposViolencia(List<Ocorrencia> ocorrencias) {
        if (ocorrencias.isEmpty()) {
            return Map.of();
        }
        Set<Integer> ids = ocorrencias.stream()
                .map(Ocorrencia::getTipoViolenciaId)
                .collect(Collectors.toSet());
        return catalogoInternalFacade.listarTiposViolenciaPorIds(ids).stream()
                .collect(Collectors.toMap(TipoViolenciaDto::id, Function.identity(), (a, _) -> a));
    }

    private Map<Integer, TipoViolenciaDto> carregarTiposViolenciaDeAgrupamentos(
            List<AgrupamentoDiagnosticoProjection> agrupamentos
    ) {
        if (agrupamentos.isEmpty()) {
            return Map.of();
        }
        Set<Integer> ids = agrupamentos.stream()
                .map(AgrupamentoDiagnosticoProjection::getTipoViolenciaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return catalogoInternalFacade.listarTiposViolenciaPorIds(ids).stream()
                .collect(Collectors.toMap(TipoViolenciaDto::id, Function.identity(), (a, _) -> a));
    }

    private Map<Integer, MarcadorVulnerabilidadeDto> carregarMarcadoresDeAgrupamentos(
            List<AgrupamentoDiagnosticoProjection> agrupamentos
    ) {
        if (agrupamentos.isEmpty()) {
            return Map.of();
        }
        Set<Integer> ids = agrupamentos.stream()
                .map(AgrupamentoDiagnosticoProjection::getMarcadorVulnerabilidadeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return catalogoInternalFacade.listarMarcadoresPorIds(ids).stream()
                .collect(Collectors.toMap(MarcadorVulnerabilidadeDto::id, Function.identity(), (a, _) -> a));
    }
}
