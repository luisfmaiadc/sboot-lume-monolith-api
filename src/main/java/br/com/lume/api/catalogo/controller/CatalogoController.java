package br.com.lume.api.catalogo.controller;

import br.com.lume.api.catalogo.mapper.CatalogoMapper;
import br.com.lume.api.catalogo.service.CatalogoService;
import br.com.lume.api.contract.CatalogosApi;
import br.com.lume.api.model.CategoriaEstabelecimentoResponse;
import br.com.lume.api.model.MarcadorVulnerabilidadeResponse;
import br.com.lume.api.model.TipoViolenciaResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CatalogoController implements CatalogosApi {

    private static final CacheControl CACHE_CATALOGO = CacheControl.maxAge(1, TimeUnit.HOURS)
            .cachePublic()
            .staleWhileRevalidate(10, TimeUnit.MINUTES);

    private final CatalogoService catalogoService;
    private final CatalogoMapper catalogoMapper;

    @Override
    public ResponseEntity<List<TipoViolenciaResponse>> listarTiposViolencia() {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CATALOGO)
                .body(catalogoMapper.toTipoViolenciaResponseListFromDto(catalogoService.listarTiposViolenciaAtivos()));
    }

    @Override
    public ResponseEntity<List<MarcadorVulnerabilidadeResponse>> listarMarcadoresVulnerabilidade() {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CATALOGO)
                .body(catalogoMapper.toMarcadorResponseListFromDto(catalogoService.listarMarcadoresVulnerabilidadeAtivos()));
    }

    @Override
    public ResponseEntity<List<CategoriaEstabelecimentoResponse>> listarCategoriasEstabelecimentos() {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CATALOGO)
                .body(catalogoMapper.toCategoriaResponseListFromDto(catalogoService.listarCategoriasEstabelecimentosAtivas()));
    }
}
