package br.com.lume.api.catalogo.mapper;

import br.com.lume.api.catalogo.domain.CategoriaEstabelecimento;
import br.com.lume.api.catalogo.domain.MarcadorVulnerabilidade;
import br.com.lume.api.catalogo.domain.TipoViolencia;
import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.model.CategoriaEstabelecimentoResponse;
import br.com.lume.api.model.MarcadorVulnerabilidadeResponse;
import br.com.lume.api.model.TipoViolenciaResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CatalogoMapper {

    TipoViolenciaResponse toResponse(TipoViolencia tipo);

    TipoViolenciaResponse toResponse(TipoViolenciaDto dto);

    TipoViolenciaDto toDto(TipoViolencia tipo);

    List<TipoViolenciaResponse> toTipoViolenciaResponseList(List<TipoViolencia> tipos);

    List<TipoViolenciaResponse> toTipoViolenciaResponseListFromDto(List<TipoViolenciaDto> dtos);

    List<TipoViolenciaDto> toTipoViolenciaDtoList(List<TipoViolencia> tipos);

    MarcadorVulnerabilidadeResponse toResponse(MarcadorVulnerabilidade marcador);

    MarcadorVulnerabilidadeResponse toResponse(MarcadorVulnerabilidadeDto dto);

    MarcadorVulnerabilidadeDto toDto(MarcadorVulnerabilidade marcador);

    List<MarcadorVulnerabilidadeResponse> toMarcadorResponseList(List<MarcadorVulnerabilidade> marcadores);

    List<MarcadorVulnerabilidadeResponse> toMarcadorResponseListFromDto(List<MarcadorVulnerabilidadeDto> dtos);

    List<MarcadorVulnerabilidadeDto> toMarcadorDtoList(List<MarcadorVulnerabilidade> marcadores);

    CategoriaEstabelecimentoResponse toResponse(CategoriaEstabelecimento categoria);

    CategoriaEstabelecimentoResponse toResponse(CategoriaEstabelecimentoDto dto);

    CategoriaEstabelecimentoDto toDto(CategoriaEstabelecimento categoria);

    List<CategoriaEstabelecimentoResponse> toCategoriaResponseList(List<CategoriaEstabelecimento> categorias);

    List<CategoriaEstabelecimentoResponse> toCategoriaResponseListFromDto(List<CategoriaEstabelecimentoDto> dtos);

    List<CategoriaEstabelecimentoDto> toCategoriaDtoList(List<CategoriaEstabelecimento> categorias);
}
