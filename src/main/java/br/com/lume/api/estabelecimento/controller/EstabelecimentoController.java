package br.com.lume.api.estabelecimento.controller;

import br.com.lume.api.contract.EstabelecimentosApi;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoDetalheDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoPageDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.estabelecimento.mapper.EstabelecimentoMapper;
import br.com.lume.api.estabelecimento.service.EstabelecimentoService;
import br.com.lume.api.model.AtualizacaoEstabelecimentoRequest;
import br.com.lume.api.model.CriacaoEstabelecimentoRequest;
import br.com.lume.api.model.EstabelecimentoAutocompleteResponse;
import br.com.lume.api.model.EstabelecimentoDetalheResponse;
import br.com.lume.api.model.EstabelecimentoPageResult;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.OcorrenciaResumoPageResult;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class EstabelecimentoController implements EstabelecimentosApi {

    private final EstabelecimentoService estabelecimentoService;
    private final EstabelecimentoMapper estabelecimentoMapper;

    @Override
    public ResponseEntity<List<EstabelecimentoAutocompleteResponse>> autocompleteEstabelecimentos(
            String termo,
            String cidade,
            Integer limite
    ) {
        List<EstabelecimentoResumoDto> dtos = estabelecimentoService.autocompleteEstabelecimentos(termo, cidade, limite);
        return ResponseEntity.ok(estabelecimentoMapper.toAutocompleteResponseListFromDto(dtos));
    }

    @Override
    public ResponseEntity<EstabelecimentoPageResult> listarEstabelecimentos(
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
        EstabelecimentoPageDto pageDto = estabelecimentoService.listarEstabelecimentos(
                termo,
                categoriaId,
                cidade,
                bairro,
                latitude,
                longitude,
                raioMetros,
                page,
                size
        );
        return ResponseEntity.ok(estabelecimentoMapper.toPageResult(pageDto));
    }

    @Override
    public ResponseEntity<EstabelecimentoDetalheResponse> cadastrarEstabelecimento(CriacaoEstabelecimentoRequest request) {
        EstabelecimentoDetalheDto dto =
                estabelecimentoService.cadastrarEstabelecimento(estabelecimentoMapper.toCommand(request));
        EstabelecimentoDetalheResponse response = estabelecimentoMapper.toDetalheResponse(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/v1/estabelecimentos/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<EstabelecimentoDetalheResponse> obterMeuEstabelecimento() {
        return estabelecimentoService.obterMeuEstabelecimento()
                .map(estabelecimentoMapper::toDetalheResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Override
    public ResponseEntity<EstabelecimentoDetalheResponse> obterEstabelecimentoPorId(UUID id) {
        EstabelecimentoDetalheDto dto = estabelecimentoService.obterEstabelecimentoPorId(id);
        return ResponseEntity.ok(estabelecimentoMapper.toDetalheResponse(dto));
    }

    @Override
    public ResponseEntity<EstabelecimentoDetalheResponse> atualizarEstabelecimento(UUID id, AtualizacaoEstabelecimentoRequest request) {
        EstabelecimentoDetalheDto dto =
                estabelecimentoService.atualizarEstabelecimento(id, estabelecimentoMapper.toCommand(request));
        return ResponseEntity.ok(estabelecimentoMapper.toDetalheResponse(dto));
    }

    @Override
    public ResponseEntity<OcorrenciaResumoPageResult> listarOcorrenciasEstabelecimento(
            UUID id,
            Integer tipoViolenciaId,
            Integer marcadorId,
            FaixaHorario faixaHorario,
            Integer page,
            Integer size
    ) {
        OcorrenciaResumoPagePortDto pageDto = estabelecimentoService.listarOcorrenciasEstabelecimento(
                id,
                tipoViolenciaId,
                marcadorId,
                faixaHorario != null ? faixaHorario.name() : null,
                page,
                size
        );
        return ResponseEntity.ok(estabelecimentoMapper.toOcorrenciaResumoPageResult(pageDto));
    }
}
