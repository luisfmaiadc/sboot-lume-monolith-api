package br.com.lume.api.ocorrencia.controller;

import br.com.lume.api.contract.SegurancaApi;
import br.com.lume.api.model.DiagnosticoSegurancaResponse;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.PontoMapaOcorrenciaResponse;
import br.com.lume.api.ocorrencia.dto.DiagnosticoSegurancaDto;
import br.com.lume.api.ocorrencia.dto.PontoMapaDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.service.SegurancaTerritorialService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SegurancaController implements SegurancaApi {

    private final SegurancaTerritorialService segurancaTerritorialService;
    private final OcorrenciaMapper ocorrenciaMapper;

    @Override
    public ResponseEntity<DiagnosticoSegurancaResponse> obterDiagnosticoRaio(
            Double latitude,
            Double longitude,
            Integer raioMetros
    ) {
        DiagnosticoSegurancaDto dto = segurancaTerritorialService.obterDiagnosticoRaio(latitude, longitude, raioMetros);
        return ResponseEntity.ok(ocorrenciaMapper.toDiagnosticoResponse(dto));
    }

    @Override
    public ResponseEntity<List<PontoMapaOcorrenciaResponse>> listarOcorrenciasParaMapa(
            Double latitude,
            Double longitude,
            Integer raioMetros,
            Integer dias,
            Integer tipoViolenciaId,
            Integer marcadorId,
            FaixaHorario faixaHorario
    ) {
        List<PontoMapaDto> dtos = segurancaTerritorialService.listarOcorrenciasParaMapa(
                latitude,
                longitude,
                raioMetros,
                dias,
                tipoViolenciaId,
                marcadorId,
                ocorrenciaMapper.toDomain(faixaHorario)
        );
        return ResponseEntity.ok(ocorrenciaMapper.toPontoMapaResponseListFromDto(dtos));
    }
}
