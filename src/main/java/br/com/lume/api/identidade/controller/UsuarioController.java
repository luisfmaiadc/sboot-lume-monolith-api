package br.com.lume.api.identidade.controller;

import br.com.lume.api.contract.UsuariosApi;
import br.com.lume.api.identidade.dto.UsuarioDetalheDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.service.UsuarioService;
import br.com.lume.api.model.AtualizacaoUsuarioRequest;
import br.com.lume.api.model.UsuarioDetalheResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UsuarioController implements UsuariosApi {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    @Override
    public ResponseEntity<UsuarioDetalheResponse> obterPerfilUsuarioLogado() {
        UsuarioDetalheDto dto = usuarioService.obterPerfilLogado();
        return ResponseEntity.ok(usuarioMapper.toDetalheResponse(dto));
    }

    @Override
    public ResponseEntity<UsuarioDetalheResponse> atualizarPerfilUsuarioLogado(AtualizacaoUsuarioRequest request) {
        UsuarioDetalheDto dto = usuarioService.atualizarPerfilLogado(usuarioMapper.toCommand(request));
        return ResponseEntity.ok(usuarioMapper.toDetalheResponse(dto));
    }
}
