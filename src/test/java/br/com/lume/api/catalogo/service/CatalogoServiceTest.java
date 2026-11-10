package br.com.lume.api.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.lume.api.catalogo.domain.CategoriaEstabelecimento;
import br.com.lume.api.catalogo.domain.MarcadorVulnerabilidade;
import br.com.lume.api.catalogo.domain.TipoViolencia;
import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.catalogo.mapper.CatalogoMapper;
import br.com.lume.api.catalogo.repository.CategoriaEstabelecimentoRepository;
import br.com.lume.api.catalogo.repository.MarcadorVulnerabilidadeRepository;
import br.com.lume.api.catalogo.repository.TipoViolenciaRepository;
import br.com.lume.api.shared.exception.BusinessRuleException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoService - Testes Unitários")
class CatalogoServiceTest {

    @Mock
    private TipoViolenciaRepository tipoViolenciaRepository;

    @Mock
    private MarcadorVulnerabilidadeRepository marcadorVulnerabilidadeRepository;

    @Mock
    private CategoriaEstabelecimentoRepository categoriaEstabelecimentoRepository;

    @Spy
    private CatalogoMapper catalogoMapper = Mappers.getMapper(CatalogoMapper.class);

    @InjectMocks
    private CatalogoService catalogoService;

    @Nested
    @DisplayName("Tipos de Violência (Listagem Pública e CatalogoInternalFacade)")
    class TiposViolenciaTests {

        @Test
        @DisplayName("Deve listar tipos de violência ativos ordenados e mapeados para TipoViolenciaDto")
        void deveListarTiposViolenciaAtivos() {
            TipoViolencia t1 = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, true);
            TipoViolencia t2 = TipoViolencia.criar(2, "AGRESSAO_FISICA", "Agressão Física", 8, 2, true);
            when(tipoViolenciaRepository.findAllByAtivoTrueOrderByOrdemExibicaoAscNomeAsc())
                    .thenReturn(List.of(t1, t2));

            List<TipoViolenciaDto> resultado = catalogoService.listarTiposViolenciaAtivos();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).id()).isEqualTo(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("ASSEDIO_VERBAL");
            assertThat(resultado.get(0).nome()).isEqualTo("Assédio Verbal");
            assertThat(resultado.get(0).pesoGravidade()).isEqualTo(2);
            assertThat(resultado.get(0).ordemExibicao()).isEqualTo(1);
            verify(tipoViolenciaRepository).findAllByAtivoTrueOrderByOrdemExibicaoAscNomeAsc();
        }

        @Test
        @DisplayName("Deve buscar tipo de violência ativo por ID ou falhar com BusinessRuleException")
        void deveBuscarTipoViolenciaAtivoOuFalhar() {
            TipoViolencia tipo = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, true);
            when(tipoViolenciaRepository.findByIdAndAtivoTrue(1)).thenReturn(Optional.of(tipo));
            when(tipoViolenciaRepository.findByIdAndAtivoTrue(99)).thenReturn(Optional.empty());

            TipoViolenciaDto dto = catalogoService.buscarTipoViolenciaAtivoOuFalhar(1);

            assertThat(dto.id()).isEqualTo(1);
            assertThat(dto.codigo()).isEqualTo("ASSEDIO_VERBAL");
            assertThat(dto.pesoGravidade()).isEqualTo(2);
            assertThat(dto.ativo()).isTrue();

            assertThatThrownBy(() -> catalogoService.buscarTipoViolenciaAtivoOuFalhar(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do tipo de violência é obrigatório.");

            assertThatThrownBy(() -> catalogoService.buscarTipoViolenciaAtivoOuFalhar(99))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Tipo de violência informado não existe ou está inativo.");
        }

        @Test
        @DisplayName("Deve buscar tipo de violência por ID retornando Optional e tratar ID nulo")
        void deveBuscarTipoViolenciaPorId() {
            TipoViolencia tipo = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, false);
            when(tipoViolenciaRepository.findById(1)).thenReturn(Optional.of(tipo));

            assertThat(catalogoService.buscarTipoViolenciaPorId(null)).isEmpty();
            verifyNoInteractions(tipoViolenciaRepository);

            Optional<TipoViolenciaDto> resultado = catalogoService.buscarTipoViolenciaPorId(1);
            assertThat(resultado).isPresent();
            assertThat(resultado.get().id()).isEqualTo(1);
            assertThat(resultado.get().ativo()).isFalse();
        }

        @Test
        @DisplayName("Deve listar tipos de violência por coleção de IDs e retornar lista vazia sem acionar banco quando nula/vazia")
        void deveListarTiposViolenciaPorIds() {
            assertThat(catalogoService.listarTiposViolenciaPorIds(null)).isEmpty();
            assertThat(catalogoService.listarTiposViolenciaPorIds(Collections.emptyList())).isEmpty();
            verifyNoInteractions(tipoViolenciaRepository);

            TipoViolencia tipo = TipoViolencia.criar(1, "ASSEDIO_VERBAL", "Assédio Verbal", 2, 1, true);
            when(tipoViolenciaRepository.findAllById(List.of(1))).thenReturn(List.of(tipo));

            List<TipoViolenciaDto> resultado = catalogoService.listarTiposViolenciaPorIds(List.of(1));
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("ASSEDIO_VERBAL");
        }
    }

    @Nested
    @DisplayName("Marcadores de Vulnerabilidade (Listagem Pública e CatalogoInternalFacade)")
    class MarcadoresVulnerabilidadeTests {

        @Test
        @DisplayName("Deve listar marcadores de vulnerabilidade ativos ordenados por ID")
        void deveListarMarcadoresAtivos() {
            MarcadorVulnerabilidade m1 = MarcadorVulnerabilidade.criar(
                    1, "MISOGINIA", "Misoginia", "Ataque motivado por gênero", true
            );
            when(marcadorVulnerabilidadeRepository.findAllByAtivoTrueOrderByIdAsc())
                    .thenReturn(List.of(m1));

            List<MarcadorVulnerabilidadeDto> resultado = catalogoService.listarMarcadoresVulnerabilidadeAtivos();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).id()).isEqualTo(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("MISOGINIA");
            assertThat(resultado.get(0).nome()).isEqualTo("Misoginia");
            assertThat(resultado.get(0).descricao()).isEqualTo("Ataque motivado por gênero");
        }

        @Test
        @DisplayName("Deve buscar marcador ativo por ID ou falhar com BusinessRuleException")
        void deveBuscarMarcadorAtivoOuFalhar() {
            MarcadorVulnerabilidade marcador = MarcadorVulnerabilidade.criar(
                    1, "MISOGINIA", "Misoginia", "Desc", true
            );
            when(marcadorVulnerabilidadeRepository.findByIdAndAtivoTrue(1)).thenReturn(Optional.of(marcador));
            when(marcadorVulnerabilidadeRepository.findByIdAndAtivoTrue(99)).thenReturn(Optional.empty());

            MarcadorVulnerabilidadeDto dto = catalogoService.buscarMarcadorAtivoOuFalhar(1);

            assertThat(dto.id()).isEqualTo(1);
            assertThat(dto.codigo()).isEqualTo("MISOGINIA");
            assertThat(dto.ativo()).isTrue();

            assertThatThrownBy(() -> catalogoService.buscarMarcadorAtivoOuFalhar(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do marcador de vulnerabilidade é obrigatório.");

            assertThatThrownBy(() -> catalogoService.buscarMarcadorAtivoOuFalhar(99))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Marcador de vulnerabilidade informado não existe ou está inativo.");
        }

        @Test
        @DisplayName("Deve buscar marcador por ID retornando Optional e tratar ID nulo")
        void deveBuscarMarcadorPorId() {
            assertThat(catalogoService.buscarMarcadorPorId(null)).isEmpty();
            verifyNoInteractions(marcadorVulnerabilidadeRepository);

            MarcadorVulnerabilidade marcador = MarcadorVulnerabilidade.criar(
                    2, "RACISMO", "Racismo", "Desc", true
            );
            when(marcadorVulnerabilidadeRepository.findById(2)).thenReturn(Optional.of(marcador));

            Optional<MarcadorVulnerabilidadeDto> resultado = catalogoService.buscarMarcadorPorId(2);
            assertThat(resultado).isPresent();
            assertThat(resultado.get().codigo()).isEqualTo("RACISMO");
        }

        @Test
        @DisplayName("Deve listar marcadores por coleção de IDs e retornar lista vazia sem acionar banco quando nula/vazia")
        void deveListarMarcadoresPorIds() {
            assertThat(catalogoService.listarMarcadoresPorIds(null)).isEmpty();
            assertThat(catalogoService.listarMarcadoresPorIds(List.of())).isEmpty();
            verifyNoInteractions(marcadorVulnerabilidadeRepository);

            MarcadorVulnerabilidade marcador = MarcadorVulnerabilidade.criar(
                    1, "MISOGINIA", "Misoginia", "Desc", true
            );
            when(marcadorVulnerabilidadeRepository.findAllById(List.of(1))).thenReturn(List.of(marcador));

            List<MarcadorVulnerabilidadeDto> resultado = catalogoService.listarMarcadoresPorIds(List.of(1));
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("MISOGINIA");
        }
    }

    @Nested
    @DisplayName("Categorias de Estabelecimento (Listagem Pública e CatalogoInternalFacade)")
    class CategoriasEstabelecimentoTests {

        @Test
        @DisplayName("Deve listar categorias de estabelecimento ativas ordenadas por nome")
        void deveListarCategoriasAtivas() {
            CategoriaEstabelecimento c1 = CategoriaEstabelecimento.criar(
                    1, "BAR_RESTAURANTE", "Bares e Restaurantes", true
            );
            when(categoriaEstabelecimentoRepository.findAllByAtivoTrueOrderByNomeAsc())
                    .thenReturn(List.of(c1));

            List<CategoriaEstabelecimentoDto> resultado = catalogoService.listarCategoriasEstabelecimentosAtivas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).id()).isEqualTo(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("BAR_RESTAURANTE");
            assertThat(resultado.get(0).nome()).isEqualTo("Bares e Restaurantes");
        }

        @Test
        @DisplayName("Deve buscar categoria ativa por ID ou falhar com BusinessRuleException")
        void deveBuscarCategoriaAtivaOuFalhar() {
            CategoriaEstabelecimento categoria = CategoriaEstabelecimento.criar(
                    1, "BAR_RESTAURANTE", "Bares e Restaurantes", true
            );
            when(categoriaEstabelecimentoRepository.findByIdAndAtivoTrue(1)).thenReturn(Optional.of(categoria));
            when(categoriaEstabelecimentoRepository.findByIdAndAtivoTrue(99)).thenReturn(Optional.empty());

            CategoriaEstabelecimentoDto dto = catalogoService.buscarCategoriaAtivaOuFalhar(1);

            assertThat(dto.id()).isEqualTo(1);
            assertThat(dto.codigo()).isEqualTo("BAR_RESTAURANTE");
            assertThat(dto.ativo()).isTrue();

            assertThatThrownBy(() -> catalogoService.buscarCategoriaAtivaOuFalhar(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador da categoria de estabelecimento é obrigatório.");

            assertThatThrownBy(() -> catalogoService.buscarCategoriaAtivaOuFalhar(99))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Categoria de estabelecimento informada não existe ou está inativa.");
        }

        @Test
        @DisplayName("Deve buscar categoria por ID retornando Optional e tratar ID nulo")
        void deveBuscarCategoriaPorId() {
            assertThat(catalogoService.buscarCategoriaPorId(null)).isEmpty();
            verifyNoInteractions(categoriaEstabelecimentoRepository);

            CategoriaEstabelecimento categoria = CategoriaEstabelecimento.criar(
                    3, "CASA_NOTURNA", "Casas Noturnas", true
            );
            when(categoriaEstabelecimentoRepository.findById(3)).thenReturn(Optional.of(categoria));

            Optional<CategoriaEstabelecimentoDto> resultado = catalogoService.buscarCategoriaPorId(3);
            assertThat(resultado).isPresent();
            assertThat(resultado.get().codigo()).isEqualTo("CASA_NOTURNA");
        }

        @Test
        @DisplayName("Deve listar categorias por coleção de IDs e retornar lista vazia sem acionar banco quando nula/vazia")
        void deveListarCategoriasPorIds() {
            assertThat(catalogoService.listarCategoriasPorIds(null)).isEmpty();
            assertThat(catalogoService.listarCategoriasPorIds(List.of())).isEmpty();
            verifyNoInteractions(categoriaEstabelecimentoRepository);

            CategoriaEstabelecimento categoria = CategoriaEstabelecimento.criar(
                    1, "BAR_RESTAURANTE", "Bares e Restaurantes", true
            );
            when(categoriaEstabelecimentoRepository.findAllById(List.of(1))).thenReturn(List.of(categoria));

            List<CategoriaEstabelecimentoDto> resultado = catalogoService.listarCategoriasPorIds(List.of(1));
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("BAR_RESTAURANTE");
        }
    }
}
