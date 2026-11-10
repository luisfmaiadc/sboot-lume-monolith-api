package br.com.lume.api.ocorrencia.service;

import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.shared.config.ReputacaoProperties;
import br.com.lume.api.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculadoraReputacaoRiscoService {

    public static final int MAXIMO_VALIDACOES_COMPUTADAS = 10;

    private static final BigDecimal SCORE_ESTABELECIMENTO_INICIAL = new BigDecimal("5.00");
    private static final BigDecimal SCORE_ESTABELECIMENTO_MINIMO = new BigDecimal("0.00");
    private static final BigDecimal COEFICIENTE_IMPACTO_ESTABELECIMENTO = new BigDecimal("0.10");

    private static final BigDecimal SCORE_RISCO_MINIMO = new BigDecimal("0.00");
    private static final BigDecimal SCORE_RISCO_MAXIMO = new BigDecimal("10.00");
    private static final BigDecimal COEFICIENTE_IMPACTO_VIA_PUBLICA = new BigDecimal("0.20");

    private static final BigDecimal LIMIAR_RISCO_MEDIO = new BigDecimal("2.50");
    private static final BigDecimal LIMIAR_RISCO_ALTO = new BigDecimal("5.00");
    private static final BigDecimal LIMIAR_RISCO_CRITICO = new BigDecimal("7.50");

    private final ReputacaoProperties reputacaoProperties;

    public BigDecimal calcularFatorDecaimentoTemporal(LocalDate dataOcorrencia, LocalDate dataReferencia) {
        if (dataOcorrencia == null || dataReferencia == null) {
            throw new BusinessRuleException("Datas de ocorrência e de referência são obrigatórias para o cálculo de decaimento temporal.");
        }
        long dias = Math.max(0L, ChronoUnit.DAYS.between(dataOcorrencia, dataReferencia));
        if (dias <= 30) {
            return reputacaoProperties.fatorDias30();
        }
        if (dias <= 90) {
            return reputacaoProperties.fatorDias90();
        }
        if (dias <= 365) {
            return reputacaoProperties.fatorDias365();
        }
        return reputacaoProperties.fatorResidual();
    }

    public BigDecimal calcularImpactoOcorrencia(
            int pesoGravidade,
            LocalDate dataOcorrencia,
            int totalValidacoes,
            LocalDate dataReferencia
    ) {
        if (pesoGravidade <= 0) {
            throw new BusinessRuleException("Peso de gravidade deve ser positivo.");
        }
        if (totalValidacoes < 0) {
            throw new BusinessRuleException("Total de validações não pode ser negativo.");
        }

        BigDecimal peso = BigDecimal.valueOf(pesoGravidade);
        BigDecimal fatorTempo = calcularFatorDecaimentoTemporal(dataOcorrencia, dataReferencia);
        int validacoesComputadas = Math.min(totalValidacoes, MAXIMO_VALIDACOES_COMPUTADAS);
        BigDecimal multiplicadorValidacao = BigDecimal.ONE.add(
                BigDecimal.valueOf(validacoesComputadas).multiply(reputacaoProperties.multiplicadorValidacao())
        );

        return peso
                .multiply(fatorTempo)
                .multiply(multiplicadorValidacao)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularScoreEstabelecimento(BigDecimal somaImpactos) {
        if (somaImpactos == null || somaImpactos.compareTo(BigDecimal.ZERO) <= 0) {
            return SCORE_ESTABELECIMENTO_INICIAL;
        }
        BigDecimal penalidade = somaImpactos.multiply(COEFICIENTE_IMPACTO_ESTABELECIMENTO);
        BigDecimal calculado = SCORE_ESTABELECIMENTO_INICIAL
                .subtract(penalidade)
                .setScale(2, RoundingMode.HALF_UP);
        return calculado.max(SCORE_ESTABELECIMENTO_MINIMO);
    }

    public BigDecimal calcularScoreRiscoViaPublica(BigDecimal somaImpactos) {
        if (somaImpactos == null || somaImpactos.compareTo(BigDecimal.ZERO) <= 0) {
            return SCORE_RISCO_MINIMO;
        }
        BigDecimal calculado = somaImpactos
                .multiply(COEFICIENTE_IMPACTO_VIA_PUBLICA)
                .setScale(2, RoundingMode.HALF_UP);
        return calculado.min(SCORE_RISCO_MAXIMO);
    }

    public NivelRisco classificarNivelRisco(BigDecimal scoreRisco) {
        if (scoreRisco == null || scoreRisco.compareTo(LIMIAR_RISCO_MEDIO) < 0) {
            return NivelRisco.BAIXO;
        }
        if (scoreRisco.compareTo(LIMIAR_RISCO_ALTO) < 0) {
            return NivelRisco.MEDIO;
        }
        if (scoreRisco.compareTo(LIMIAR_RISCO_CRITICO) < 0) {
            return NivelRisco.ALTO;
        }
        return NivelRisco.CRITICO;
    }
}