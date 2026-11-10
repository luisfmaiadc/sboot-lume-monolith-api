package br.com.lume.api.ocorrencia.controller;

import br.com.lume.api.contract.OcorrenciasApi;
import br.com.lume.api.model.AtualizacaoStatusOcorrenciaRequest;
import br.com.lume.api.model.MinhaOcorrenciaPageResult;
import br.com.lume.api.model.OcorrenciaDetalheResponse;
import br.com.lume.api.model.RegistroOcorrenciaRequest;
import br.com.lume.api.model.ValidacaoOcorrenciaRequest;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaPageDto;
import br.com.lume.api.ocorrencia.dto.OcorrenciaDetalheDto;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.ocorrencia.service.OcorrenciaService;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class OcorrenciaController implements OcorrenciasApi {

    private final OcorrenciaService ocorrenciaService;
    private final OcorrenciaMapper ocorrenciaMapper;

    @Override
    public ResponseEntity<OcorrenciaDetalheResponse> registrarOcorrencia(RegistroOcorrenciaRequest request) {
        OcorrenciaDetalheDto dto = ocorrenciaService.registrarOcorrencia(ocorrenciaMapper.toCommand(request));
        OcorrenciaDetalheResponse response = ocorrenciaMapper.toDetalheResponse(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/v1/ocorrencias/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<OcorrenciaDetalheResponse> obterOcorrenciaPorId(UUID id) {
        OcorrenciaDetalheDto dto = ocorrenciaService.obterOcorrenciaPorId(id);
        return ResponseEntity.ok(ocorrenciaMapper.toDetalheResponse(dto));
    }

    @Override
    public ResponseEntity<MinhaOcorrenciaPageResult> listarMinhasOcorrencias(Integer page, Integer size) {
        MinhaOcorrenciaPageDto pageDto = ocorrenciaService.listarMinhasOcorrencias(page, size);
        return ResponseEntity.ok(ocorrenciaMapper.toMinhaOcorrenciaPageResult(pageDto));
    }

    @Override
    public ResponseEntity<Void> excluirOcorrencia(UUID id) {
        ocorrenciaService.excluirOcorrencia(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('MODERADOR', 'ADMIN')")
    public ResponseEntity<OcorrenciaDetalheResponse> atualizarStatusOcorrencia(UUID id, AtualizacaoStatusOcorrenciaRequest request) {
        OcorrenciaDetalheDto dto = ocorrenciaService.atualizarStatusOcorrencia(id, ocorrenciaMapper.toCommand(request));
        return ResponseEntity.ok(ocorrenciaMapper.toDetalheResponse(dto));
    }

    @Override
    public ResponseEntity<Void> validarOcorrencia(UUID id, ValidacaoOcorrenciaRequest request) {
        ocorrenciaService.validarOcorrencia(id, ocorrenciaMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/v1/ocorrencias/{id}/validacoes")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @Override
    public ResponseEntity<Void> cancelarValidacaoOcorrencia(UUID id) {
        ocorrenciaService.cancelarValidacaoOcorrencia(id);
        return ResponseEntity.noContent().build();
    }
}
