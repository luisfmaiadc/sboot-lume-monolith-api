package br.com.lume.api.estabelecimento.mapper;

import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.estabelecimento.command.AtualizacaoEstabelecimentoCommand;
import br.com.lume.api.estabelecimento.command.CriacaoEstabelecimentoCommand;
import br.com.lume.api.estabelecimento.domain.Estabelecimento;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoDetalheDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoPageDto;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.model.AtualizacaoEstabelecimentoRequest;
import br.com.lume.api.model.CategoriaEstabelecimentoResponse;
import br.com.lume.api.model.ContagemItemResponse;
import br.com.lume.api.model.CriacaoEstabelecimentoRequest;
import br.com.lume.api.model.DistribuicaoTurnoResponse;
import br.com.lume.api.model.EstabelecimentoAutocompleteResponse;
import br.com.lume.api.model.EstabelecimentoDetalheResponse;
import br.com.lume.api.model.EstabelecimentoPageResult;
import br.com.lume.api.model.EstabelecimentoResumoResponse;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.OcorrenciaResumoPageResult;
import br.com.lume.api.model.OcorrenciaResumoResponse;
import br.com.lume.api.shared.port.ContagemItemDto;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import br.com.lume.api.shared.port.EstatisticasOcorrenciaEstabelecimentoDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPagePortDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPortDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EstabelecimentoMapper {

    CriacaoEstabelecimentoCommand toCommand(CriacaoEstabelecimentoRequest request);
    AtualizacaoEstabelecimentoCommand toCommand(AtualizacaoEstabelecimentoRequest request);

    EstabelecimentoAutocompleteResponse toAutocompleteResponse(Estabelecimento estabelecimento, String categoriaNome);
    EstabelecimentoAutocompleteResponse toAutocompleteResponse(EstabelecimentoResumoDto dto);
    List<EstabelecimentoAutocompleteResponse> toAutocompleteResponseListFromDto(List<EstabelecimentoResumoDto> dtos);

    EstabelecimentoResumoResponse toResumoResponse(Estabelecimento estabelecimento, String categoriaNome);
    EstabelecimentoResumoResponse toResumoResponse(EstabelecimentoResumoDto dto);
    List<EstabelecimentoResumoResponse> toResumoResponseListFromDto(List<EstabelecimentoResumoDto> dtos);

    EstabelecimentoPageResult toPageResult(EstabelecimentoPageDto pageDto);

    @Mapping(target = "id", source = "estabelecimento.id")
    @Mapping(target = "nome", source = "estabelecimento.nome")
    @Mapping(target = "ativo", source = "estabelecimento.ativo")
    @Mapping(target = "distribuicaoTurno", expression = "java(toDistribuicaoTurnoResponse(stats))")
    @Mapping(target = "totalOcorrenciasHistoricas", expression = "java(mapTotalOcorrencias(stats))")
    @Mapping(target = "distribuicaoTipoViolencia", source = "distribuicaoTipoViolencia")
    EstabelecimentoDetalheResponse toDetalheResponse(
            Estabelecimento estabelecimento,
            CategoriaEstabelecimentoDto categoria,
            EstatisticasOcorrenciaEstabelecimentoDto stats,
            List<ContagemItemResponse> distribuicaoTipoViolencia
    );

    EstabelecimentoDetalheResponse toDetalheResponse(EstabelecimentoDetalheDto dto);

    @Mapping(target = "id", source = "estabelecimento.id")
    @Mapping(target = "nome", source = "estabelecimento.nome")
    @Mapping(target = "ativo", source = "estabelecimento.ativo")
    @Mapping(target = "distribuicaoTurno", expression = "java(toDistribuicaoTurnoDto(stats))")
    @Mapping(target = "totalOcorrenciasHistoricas", expression = "java(mapTotalOcorrencias(stats))")
    @Mapping(target = "distribuicaoTipoViolencia", source = "distribuicaoTipoViolencia")
    EstabelecimentoDetalheDto toDetalheDto(
            Estabelecimento estabelecimento,
            CategoriaEstabelecimentoDto categoria,
            EstatisticasOcorrenciaEstabelecimentoDto stats,
            List<ContagemItemDto> distribuicaoTipoViolencia
    );

    CategoriaEstabelecimentoResponse toCategoriaResponse(CategoriaEstabelecimentoDto categoria);
    ContagemItemResponse toContagemItemResponse(ContagemItemDto dto);
    DistribuicaoTurnoResponse toDistribuicaoTurnoResponse(DistribuicaoTurnoDto dto);

    OcorrenciaResumoResponse toOcorrenciaResumoResponse(OcorrenciaResumoPortDto dto);
    OcorrenciaResumoPageResult toOcorrenciaResumoPageResult(OcorrenciaResumoPagePortDto pageDto);

    default DistribuicaoTurnoDto toDistribuicaoTurnoDto(EstatisticasOcorrenciaEstabelecimentoDto stats) {
        if (stats == null) {
            return new DistribuicaoTurnoDto(0, 0, 0, 0);
        }
        return new DistribuicaoTurnoDto(
                stats.madrugada(),
                stats.manha(),
                stats.tarde(),
                stats.noite()
        );
    }

    default DistribuicaoTurnoResponse toDistribuicaoTurnoResponse(EstatisticasOcorrenciaEstabelecimentoDto stats) {
        if (stats == null) {
            return new DistribuicaoTurnoResponse(0, 0, 0, 0);
        }
        return new DistribuicaoTurnoResponse(
                stats.madrugada(),
                stats.manha(),
                stats.tarde(),
                stats.noite()
        );
    }

    EstabelecimentoResumoDto toResumoDto(Estabelecimento estabelecimento, String categoriaNome);

    default List<EstabelecimentoAutocompleteResponse> toAutocompleteResponseList(
            List<Estabelecimento> estabelecimentos,
            Map<Integer, String> nomesCategorias
    ) {
        if (estabelecimentos == null) {
            return null;
        }
        List<EstabelecimentoAutocompleteResponse> list = new ArrayList<>(estabelecimentos.size());
        for (Estabelecimento e : estabelecimentos) {
            String catNome = nomesCategorias != null ? nomesCategorias.getOrDefault(e.getCategoriaId(), "") : "";
            list.add(toAutocompleteResponse(e, catNome));
        }
        return list;
    }

    default List<EstabelecimentoResumoResponse> toResumoResponseList(
            List<Estabelecimento> estabelecimentos,
            Map<Integer, String> nomesCategorias
    ) {
        if (estabelecimentos == null) {
            return null;
        }
        List<EstabelecimentoResumoResponse> list = new ArrayList<>(estabelecimentos.size());
        for (Estabelecimento e : estabelecimentos) {
            String catNome = nomesCategorias != null ? nomesCategorias.getOrDefault(e.getCategoriaId(), "") : "";
            list.add(toResumoResponse(e, catNome));
        }
        return list;
    }

    default List<EstabelecimentoResumoDto> toResumoDtoList(
            List<Estabelecimento> estabelecimentos,
            Map<Integer, String> nomesCategorias
    ) {
        if (estabelecimentos == null) {
            return null;
        }
        List<EstabelecimentoResumoDto> list = new ArrayList<>(estabelecimentos.size());
        for (Estabelecimento e : estabelecimentos) {
            String catNome = nomesCategorias != null ? nomesCategorias.getOrDefault(e.getCategoriaId(), "") : "";
            list.add(toResumoDto(e, catNome));
        }
        return list;
    }

    default Double mapScore(BigDecimal score) {
        return score != null ? score.doubleValue() : null;
    }

    default Integer mapTotalOcorrencias(EstatisticasOcorrenciaEstabelecimentoDto stats) {
        return stats != null ? stats.totalOcorrenciasHistoricas() : 0;
    }
}
