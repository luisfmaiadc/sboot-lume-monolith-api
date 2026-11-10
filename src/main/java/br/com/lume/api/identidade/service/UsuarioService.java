package br.com.lume.api.identidade.service;

import br.com.lume.api.identidade.api.IdentidadeInternalFacade;
import br.com.lume.api.identidade.command.AtualizacaoUsuarioCommand;
import br.com.lume.api.identidade.domain.Credencial;
import br.com.lume.api.identidade.domain.Usuario;
import br.com.lume.api.identidade.dto.UsuarioDetalheDto;
import br.com.lume.api.identidade.dto.UsuarioResumoDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.repository.CredencialRepository;
import br.com.lume.api.identidade.repository.UsuarioRepository;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.security.AuthenticatedUserProvider;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements IdentidadeInternalFacade {

    private final UsuarioRepository usuarioRepository;
    private final CredencialRepository credencialRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final Clock clock;
    private final UsuarioMapper usuarioMapper;

    @Transactional(readOnly = true)
    public UsuarioDetalheDto obterPerfilLogado() {
        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        Usuario usuario = buscarUsuarioComPerfis(usuarioId);
        Credencial credencial = buscarCredencial(usuarioId);
        return usuarioMapper.toDetalheDto(usuario, credencial.getEmail());
    }

    @Transactional
    public UsuarioDetalheDto atualizarPerfilLogado(AtualizacaoUsuarioCommand command) {
        if (command == null) {
            throw new BusinessRuleException("Dados de atualização são obrigatórios.");
        }

        UUID usuarioId = authenticatedUserProvider.getUsuarioIdAutenticado();
        Usuario usuario = buscarUsuarioComPerfis(usuarioId);
        Credencial credencial = buscarCredencial(usuarioId);

        String telefoneNormalizado = null;
        if (command.telefone() != null) {
            telefoneNormalizado = Usuario.normalizarTelefone(command.telefone());
            if (usuarioRepository.existsByTelefoneAndIdNot(telefoneNormalizado, usuarioId)) {
                throw new ConflictException("Já existe outro usuário cadastrado com o telefone informado.");
            }
        }

        usuario.atualizarDadosCadastrais(
                command.nomeCompleto(),
                telefoneNormalizado,
                command.genero(),
                command.racaEtnia(),
                command.orientacaoSexual(),
                OffsetDateTime.now(clock)
        );

        usuarioRepository.save(usuario);
        log.info("Perfil do usuário atualizado com sucesso: usuarioId={}", usuarioId);

        return usuarioMapper.toDetalheDto(usuario, credencial.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioResumoDto> buscarResumoPorId(UUID usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByIdWithPerfis(usuarioId)
                .map(usuario -> new UsuarioResumoDto(
                        usuario.getId(),
                        usuario.getNomeCompleto(),
                        usuario.isAtivo(),
                        usuario.getNomesPerfis()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean usuarioPossuiPerfil(UUID usuarioId, String nomePerfil) {
        if (usuarioId == null || nomePerfil == null || nomePerfil.isBlank()) {
            return false;
        }
        return usuarioRepository.findByIdWithPerfis(usuarioId)
                .map(usuario -> usuario.isAtivo() && usuario.possuiPerfil(nomePerfil))
                .orElse(false);
    }

    @Override
    @Transactional
    public void adquirirLockParaUsuario(UUID usuarioId) {
        if (usuarioId == null) {
            throw new ResourceNotFoundException("Usuário não encontrado.");
        }
        usuarioRepository.findByIdWithLock(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private Usuario buscarUsuarioComPerfis(UUID usuarioId) {
        return usuarioRepository.findByIdWithPerfis(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private Credencial buscarCredencial(UUID usuarioId) {
        return credencialRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Credencial do usuário não encontrada."));
    }
}
