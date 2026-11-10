package br.com.lume.api.catalogo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("CatalogoMapper - Testes Unitários")
class CatalogoMapperTest {

    private CatalogoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CatalogoMapper.class);
    }

    @Test
    @DisplayName("Deve mapear TipoViolencia para TipoViolenciaResponse e TipoViolenciaDto")
    void deveMapearTipoViolencia() {
        TipoViolencia tipo = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, true);

        TipoViolenciaResponse response = mapper.toResponse(tipo);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getCodigo()).isEqualTo("ASSEDIO_VERBAL");
        assertThat(response.getNome()).isEqualTo("Assédio Verbal");
        assertThat(response.getPesoGravidade()).isEqualTo(2);
        assertThat(response.getOrdemExibicao()).isEqualTo(1);

        TipoViolenciaDto dto = mapper.toDto(tipo);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.codigo()).isEqualTo("ASSEDIO_VERBAL");
        assertThat(dto.nome()).isEqualTo("Assédio Verbal");
        assertThat(dto.pesoGravidade()).isEqualTo(2);
        assertThat(dto.ordemExibicao()).isEqualTo(1);
        assertThat(dto.ativo()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear MarcadorVulnerabilidade para MarcadorVulnerabilidadeResponse e MarcadorVulnerabilidadeDto")
    void deveMapearMarcadorVulnerabilidade() {
        MarcadorVulnerabilidade marcador = MarcadorVulnerabilidade.criar(1, "MISOGINIA", "Misoginia", "Descricao", true);

        MarcadorVulnerabilidadeResponse response = mapper.toResponse(marcador);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getCodigo()).isEqualTo("MISOGINIA");
        assertThat(response.getNome()).isEqualTo("Misoginia");
        assertThat(response.getDescricao()).isEqualTo("Descricao");

        MarcadorVulnerabilidadeDto dto = mapper.toDto(marcador);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.codigo()).isEqualTo("MISOGINIA");
        assertThat(dto.nome()).isEqualTo("Misoginia");
        assertThat(dto.descricao()).isEqualTo("Descricao");
        assertThat(dto.ativo()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear CategoriaEstabelecimento para CategoriaEstabelecimentoResponse e CategoriaEstabelecimentoDto")
    void deveMapearCategoriaEstabelecimento() {
        CategoriaEstabelecimento categoria = CategoriaEstabelecimento.criar(1, "BAR", "Bar e Restaurante", true);

        CategoriaEstabelecimentoResponse response = mapper.toResponse(categoria);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getCodigo()).isEqualTo("BAR");
        assertThat(response.getNome()).isEqualTo("Bar e Restaurante");

        CategoriaEstabelecimentoDto dto = mapper.toDto(categoria);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.codigo()).isEqualTo("BAR");
        assertThat(dto.nome()).isEqualTo("Bar e Restaurante");
        assertThat(dto.ativo()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear listas de entidades de catálogo para listas de Response e DTO")
    void deveMapearListasDeEntidades() {
        TipoViolencia tipo = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, true);
        List<TipoViolenciaResponse> tipoResponses = mapper.toTipoViolenciaResponseList(List.of(tipo));
        assertThat(tipoResponses).hasSize(1);
        assertThat(tipoResponses.get(0).getId()).isEqualTo(1);

        List<TipoViolenciaDto> tipoDtos = mapper.toTipoViolenciaDtoList(List.of(tipo));
        assertThat(tipoDtos).hasSize(1);
        assertThat(tipoDtos.get(0).id()).isEqualTo(1);

        MarcadorVulnerabilidade marcador = MarcadorVulnerabilidade.criar(1, "MISOGINIA", "Misoginia", "Descricao", true);
        List<MarcadorVulnerabilidadeResponse> marcadorResponses = mapper.toMarcadorResponseList(List.of(marcador));
        assertThat(marcadorResponses).hasSize(1);
        assertThat(marcadorResponses.get(0).getId()).isEqualTo(1);

        List<MarcadorVulnerabilidadeDto> marcadorDtos = mapper.toMarcadorDtoList(List.of(marcador));
        assertThat(marcadorDtos).hasSize(1);
        assertThat(marcadorDtos.get(0).id()).isEqualTo(1);

        CategoriaEstabelecimento categoria = CategoriaEstabelecimento.criar(1, "BAR", "Bar e Restaurante", true);
        List<CategoriaEstabelecimentoResponse> categoriaResponses = mapper.toCategoriaResponseList(List.of(categoria));
        assertThat(categoriaResponses).hasSize(1);
        assertThat(categoriaResponses.get(0).getId()).isEqualTo(1);

        List<CategoriaEstabelecimentoDto> categoriaDtos = mapper.toCategoriaDtoList(List.of(categoria));
        assertThat(categoriaDtos).hasSize(1);
        assertThat(categoriaDtos.get(0).id()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve retornar null quando as entidades ou listas de entrada forem nulas")
    void deveRetornarNullParaEntidadesNulas() {
        assertThat(mapper.toResponse((TipoViolencia) null)).isNull();
        assertThat(mapper.toDto((TipoViolencia) null)).isNull();
        assertThat(mapper.toResponse((MarcadorVulnerabilidade) null)).isNull();
        assertThat(mapper.toDto((MarcadorVulnerabilidade) null)).isNull();
        assertThat(mapper.toResponse((CategoriaEstabelecimento) null)).isNull();
        assertThat(mapper.toDto((CategoriaEstabelecimento) null)).isNull();

        assertThat(mapper.toTipoViolenciaResponseList(null)).isNull();
        assertThat(mapper.toTipoViolenciaDtoList(null)).isNull();
        assertThat(mapper.toMarcadorResponseList(null)).isNull();
        assertThat(mapper.toMarcadorDtoList(null)).isNull();
        assertThat(mapper.toCategoriaResponseList(null)).isNull();
        assertThat(mapper.toCategoriaDtoList(null)).isNull();
    }
}
