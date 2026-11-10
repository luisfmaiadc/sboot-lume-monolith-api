package br.com.lume.api.catalogo.service;

import br.com.lume.api.catalogo.api.CatalogoInternalFacade;
import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.catalogo.mapper.CatalogoMapper;
import br.com.lume.api.catalogo.repository.CategoriaEstabelecimentoRepository;
import br.com.lume.api.catalogo.repository.MarcadorVulnerabilidadeRepository;
import br.com.lume.api.catalogo.repository.TipoViolenciaRepository;
import br.com.lume.api.shared.config.CacheConfig;
import br.com.lume.api.shared.exception.BusinessRuleException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService implements CatalogoInternalFacade {

    private final TipoViolenciaRepository tipoViolenciaRepository;
    private final MarcadorVulnerabilidadeRepository marcadorVulnerabilidadeRepository;
    private final CategoriaEstabelecimentoRepository categoriaEstabelecimentoRepository;
    private final CatalogoMapper catalogoMapper;

    @Cacheable(CacheConfig.CACHE_TIPOS_VIOLENCIA)
    public List<TipoViolenciaDto> listarTiposViolenciaAtivos() {
        return catalogoMapper.toTipoViolenciaDtoList(
                tipoViolenciaRepository.findAllByAtivoTrueOrderByOrdemExibicaoAscNomeAsc()
        );
    }

    @Cacheable(CacheConfig.CACHE_MARCADORES_VULNERABILIDADE)
    public List<MarcadorVulnerabilidadeDto> listarMarcadoresVulnerabilidadeAtivos() {
        return catalogoMapper.toMarcadorDtoList(
                marcadorVulnerabilidadeRepository.findAllByAtivoTrueOrderByIdAsc()
        );
    }

    @Cacheable(CacheConfig.CACHE_CATEGORIAS_ESTABELECIMENTOS)
    public List<CategoriaEstabelecimentoDto> listarCategoriasEstabelecimentosAtivas() {
        return catalogoMapper.toCategoriaDtoList(
                categoriaEstabelecimentoRepository.findAllByAtivoTrueOrderByNomeAsc()
        );
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_TIPOS_VIOLENCIA, key = "'ativo:' + #id", condition = "#id != null")
    public TipoViolenciaDto buscarTipoViolenciaAtivoOuFalhar(Integer id) {
        if (id == null) {
            throw new BusinessRuleException("Identificador do tipo de violência é obrigatório.");
        }
        return tipoViolenciaRepository.findByIdAndAtivoTrue(id)
                .map(catalogoMapper::toDto)
                .orElseThrow(() -> new BusinessRuleException("Tipo de violência informado não existe ou está inativo."));
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_TIPOS_VIOLENCIA, key = "'id:' + #id", condition = "#id != null")
    public Optional<TipoViolenciaDto> buscarTipoViolenciaPorId(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return tipoViolenciaRepository.findById(id)
                .map(catalogoMapper::toDto);
    }

    @Override
    public List<TipoViolenciaDto> listarTiposViolenciaPorIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return catalogoMapper.toTipoViolenciaDtoList(tipoViolenciaRepository.findAllById(ids));
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_MARCADORES_VULNERABILIDADE, key = "'ativo:' + #id", condition = "#id != null")
    public MarcadorVulnerabilidadeDto buscarMarcadorAtivoOuFalhar(Integer id) {
        if (id == null) {
            throw new BusinessRuleException("Identificador do marcador de vulnerabilidade é obrigatório.");
        }
        return marcadorVulnerabilidadeRepository.findByIdAndAtivoTrue(id)
                .map(catalogoMapper::toDto)
                .orElseThrow(() -> new BusinessRuleException("Marcador de vulnerabilidade informado não existe ou está inativo."));
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_MARCADORES_VULNERABILIDADE, key = "'id:' + #id", condition = "#id != null")
    public Optional<MarcadorVulnerabilidadeDto> buscarMarcadorPorId(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return marcadorVulnerabilidadeRepository.findById(id)
                .map(catalogoMapper::toDto);
    }

    @Override
    public List<MarcadorVulnerabilidadeDto> listarMarcadoresPorIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return catalogoMapper.toMarcadorDtoList(marcadorVulnerabilidadeRepository.findAllById(ids));
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_CATEGORIAS_ESTABELECIMENTOS, key = "'ativo:' + #id", condition = "#id != null")
    public CategoriaEstabelecimentoDto buscarCategoriaAtivaOuFalhar(Integer id) {
        if (id == null) {
            throw new BusinessRuleException("Identificador da categoria de estabelecimento é obrigatório.");
        }
        return categoriaEstabelecimentoRepository.findByIdAndAtivoTrue(id)
                .map(catalogoMapper::toDto)
                .orElseThrow(() -> new BusinessRuleException("Categoria de estabelecimento informada não existe ou está inativa."));
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_CATEGORIAS_ESTABELECIMENTOS, key = "'id:' + #id", condition = "#id != null")
    public Optional<CategoriaEstabelecimentoDto> buscarCategoriaPorId(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return categoriaEstabelecimentoRepository.findById(id)
                .map(catalogoMapper::toDto);
    }

    @Override
    public List<CategoriaEstabelecimentoDto> listarCategoriasPorIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return catalogoMapper.toCategoriaDtoList(categoriaEstabelecimentoRepository.findAllById(ids));
    }
}
