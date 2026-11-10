package br.com.lume.api.identidade.controller;

import br.com.lume.api.contract.AutenticacaoApi;
import br.com.lume.api.identidade.dto.TokenResultDto;
import br.com.lume.api.identidade.dto.UsuarioAutenticadoDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.service.AutenticacaoService;
import br.com.lume.api.model.CadastroUsuarioRequest;
import br.com.lume.api.model.LoginRequest;
import br.com.lume.api.model.RefreshTokenRequest;
import br.com.lume.api.model.TokenResponse;
import br.com.lume.api.model.UsuarioResumoResponse;

import java.net.URI;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class AutenticacaoController implements AutenticacaoApi {

    private final AutenticacaoService autenticacaoService;
    private final UsuarioMapper usuarioMapper;

    @Override
    public ResponseEntity<UsuarioResumoResponse> cadastrarUsuario(CadastroUsuarioRequest request) {
        UsuarioAutenticadoDto usuario = autenticacaoService.cadastrar(usuarioMapper.toCommand(request));
        UsuarioResumoResponse response = usuarioMapper.toResumoResponse(usuario);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/v1/usuarios/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<TokenResponse> autenticarUsuario(LoginRequest loginRequest) {
        TokenResultDto resultado = autenticacaoService.autenticar(usuarioMapper.toCommand(loginRequest));
        return ResponseEntity.ok(usuarioMapper.toTokenResponse(resultado));
    }

    @Override
    public ResponseEntity<TokenResponse> renovarToken(RefreshTokenRequest refreshTokenRequest) {
        TokenResultDto resultado = autenticacaoService.renovarToken(usuarioMapper.toCommand(refreshTokenRequest));
        return ResponseEntity.ok(usuarioMapper.toTokenResponse(resultado));
    }
}
