package br.com.lume.api.catalogo.api;

import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CatalogoInternalFacade {

    TipoViolenciaDto buscarTipoViolenciaAtivoOuFalhar(Integer id);

    Optional<TipoViolenciaDto> buscarTipoViolenciaPorId(Integer id);

    List<TipoViolenciaDto> listarTiposViolenciaPorIds(Collection<Integer> ids);

    MarcadorVulnerabilidadeDto buscarMarcadorAtivoOuFalhar(Integer id);

    Optional<MarcadorVulnerabilidadeDto> buscarMarcadorPorId(Integer id);

    List<MarcadorVulnerabilidadeDto> listarMarcadoresPorIds(Collection<Integer> ids);

    CategoriaEstabelecimentoDto buscarCategoriaAtivaOuFalhar(Integer id);

    Optional<CategoriaEstabelecimentoDto> buscarCategoriaPorId(Integer id);

    List<CategoriaEstabelecimentoDto> listarCategoriasPorIds(Collection<Integer> ids);
}
