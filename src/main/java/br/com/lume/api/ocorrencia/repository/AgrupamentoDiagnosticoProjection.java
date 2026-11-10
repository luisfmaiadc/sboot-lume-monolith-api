package br.com.lume.api.ocorrencia.repository;

import java.time.LocalDate;

public interface AgrupamentoDiagnosticoProjection {

    Integer getTipoViolenciaId();

    String getFaixaHorario();

    Integer getMarcadorVulnerabilidadeId();

    LocalDate getDataOcorrencia();

    int getValidacoesComputadas();

    int getQuantidade();
}
