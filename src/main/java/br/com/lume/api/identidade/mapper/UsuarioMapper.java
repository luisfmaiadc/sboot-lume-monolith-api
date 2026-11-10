package br.com.lume.api.identidade.mapper;

import br.com.lume.api.identidade.command.AtualizacaoUsuarioCommand;
import br.com.lume.api.identidade.command.CadastroUsuarioCommand;
import br.com.lume.api.identidade.command.LoginCommand;
import br.com.lume.api.identidade.command.RefreshTokenCommand;
import br.com.lume.api.identidade.domain.Usuario;
import br.com.lume.api.identidade.dto.TokenResultDto;
import br.com.lume.api.identidade.dto.UsuarioAutenticadoDto;
import br.com.lume.api.identidade.dto.UsuarioDetalheDto;
import br.com.lume.api.model.AtualizacaoUsuarioRequest;
import br.com.lume.api.model.CadastroUsuarioRequest;
import br.com.lume.api.model.LoginRequest;
import br.com.lume.api.model.RefreshTokenRequest;
import br.com.lume.api.model.TokenResponse;
import br.com.lume.api.model.UsuarioDetalheResponse;
import br.com.lume.api.model.UsuarioResumoResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UsuarioMapper {

    br.com.lume.api.identidade.domain.Genero toDomain(br.com.lume.api.model.Genero genero);
    br.com.lume.api.model.Genero toApi(br.com.lume.api.identidade.domain.Genero genero);

    br.com.lume.api.identidade.domain.OrientacaoSexual toDomain(br.com.lume.api.model.OrientacaoSexual orientacaoSexual);
    br.com.lume.api.model.OrientacaoSexual toApi(br.com.lume.api.identidade.domain.OrientacaoSexual orientacaoSexual);

    br.com.lume.api.identidade.domain.RacaEtnia toDomain(br.com.lume.api.model.RacaEtnia racaEtnia);
    br.com.lume.api.model.RacaEtnia toApi(br.com.lume.api.identidade.domain.RacaEtnia racaEtnia);

    CadastroUsuarioCommand toCommand(CadastroUsuarioRequest request);
    LoginCommand toCommand(LoginRequest request);
    RefreshTokenCommand toCommand(RefreshTokenRequest request);
    AtualizacaoUsuarioCommand toCommand(AtualizacaoUsuarioRequest request);

    @Mapping(target = "perfis", source = "usuario.nomesPerfis")
    UsuarioResumoResponse toResumoResponse(Usuario usuario, String email);

    UsuarioResumoResponse toResumoResponse(UsuarioAutenticadoDto dto);

    TokenResponse toTokenResponse(TokenResultDto dto);

    @Mapping(target = "perfis", source = "usuario.nomesPerfis")
    UsuarioDetalheResponse toDetalheResponse(Usuario usuario, String email);

    UsuarioDetalheResponse toDetalheResponse(UsuarioDetalheDto dto);

    @Mapping(target = "perfis", source = "usuario.nomesPerfis")
    UsuarioAutenticadoDto toAutenticadoDto(Usuario usuario, String email);

    @Mapping(target = "perfis", source = "usuario.nomesPerfis")
    UsuarioDetalheDto toDetalheDto(Usuario usuario, String email);
}
