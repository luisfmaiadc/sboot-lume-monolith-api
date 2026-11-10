package br.com.lume.api.identidade.service;

import br.com.lume.api.identidade.command.CadastroUsuarioCommand;
import br.com.lume.api.identidade.command.LoginCommand;
import br.com.lume.api.identidade.command.RefreshTokenCommand;
import br.com.lume.api.identidade.domain.Credencial;
import br.com.lume.api.identidade.domain.Perfil;
import br.com.lume.api.identidade.domain.Usuario;
import br.com.lume.api.identidade.dto.TokenResultDto;
import br.com.lume.api.identidade.dto.UsuarioAutenticadoDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.repository.CredencialRepository;
import br.com.lume.api.identidade.repository.PerfilRepository;
import br.com.lume.api.identidade.repository.UsuarioRepository;
import br.com.lume.api.shared.config.LockoutProperties;
import br.com.lume.api.shared.exception.AccountLockedException;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.exception.UnauthorizedException;
import br.com.lume.api.shared.security.CpfCryptoService;
import br.com.lume.api.shared.security.JwtService;
import br.com.lume.api.shared.util.CpfValidator;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    public static final String ROLE_USUARIO = "ROLE_USUARIO";
    static final String DUMMY_PASSWORD_HASH = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final CredencialRepository credencialRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CpfCryptoService cpfCryptoService;
    private final LockoutProperties lockoutProperties;
    private final Clock clock;

    @Transactional
    public UsuarioAutenticadoDto cadastrar(CadastroUsuarioCommand command) {
        validarDadosEntrada(command);

        String emailNormalizado = normalizarEmail(command.email());
        String telefoneNormalizado = Usuario.normalizarTelefone(command.telefone());
        String cpfHash = cpfCryptoService.gerarHash(command.cpf());
        validarInexistenciaDeDuplicidades(cpfHash, telefoneNormalizado, emailNormalizado);

        OffsetDateTime agora = OffsetDateTime.now(clock);
        Usuario usuario = criarUsuarioComPerfilPadrao(command, cpfHash, telefoneNormalizado, agora);
        Credencial credencial = criarCredencial(usuario, emailNormalizado, command.senha(), agora);

        usuarioRepository.save(usuario);
        credencialRepository.save(credencial);

        log.info("Usuário cadastrado com sucesso: usuarioId={}", usuario.getId());
        return usuarioMapper.toAutenticadoDto(usuario, credencial.getEmail());
    }

    @Transactional(noRollbackFor = {UnauthorizedException.class, AccountLockedException.class})
    public TokenResultDto autenticar(LoginCommand command) {
        log.info("Iniciando processo de autenticação de credencial.");
        validarPayloadLogin(command);

        String emailNormalizado = normalizarEmail(command.email());
        Credencial credencial = buscarCredencialOuExecutarDummyVerification(emailNormalizado, command.senha());

        OffsetDateTime agora = OffsetDateTime.now(clock);
        credencial.verificarBloqueio(agora);

        validarSenha(credencial, command.senha(), agora);

        Usuario usuario = credencial.getUsuario();
        usuario.validarContaAtiva();

        registrarSucesso(credencial, agora);

        log.info("Autenticação realizada com sucesso: usuarioId={}", usuario.getId());
        return emitirTokens(usuario, credencial.getEmail());
    }

    @Transactional(readOnly = true)
    public TokenResultDto renovarToken(RefreshTokenCommand command) {
        log.info("Iniciando processo de renovação de token.");
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token é obrigatório.");
        }

        UUID usuarioId = jwtService.validarRefreshToken(command.refreshToken());

        Usuario usuario = usuarioRepository.findByIdWithPerfis(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado para renovação de token."));
        Credencial credencial = credencialRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Credencial não encontrada para renovação de token."));

        usuario.validarContaAtiva();
        credencial.verificarBloqueio(OffsetDateTime.now(clock));

        log.info("Token renovado com sucesso: usuarioId={}", usuario.getId());

        return emitirTokens(usuario, credencial.getEmail());
    }

    private TokenResultDto emitirTokens(Usuario usuario, String email) {
        List<String> perfis = usuario.getNomesPerfis();
        String accessToken = jwtService.gerarAccessToken(usuario.getId(), email, perfis);
        String refreshToken = jwtService.gerarRefreshToken(usuario.getId(), email);

        return new TokenResultDto(
                accessToken,
                refreshToken,
                TOKEN_TYPE_BEARER,
                (int) jwtService.getExpirationSeconds(),
                usuarioMapper.toAutenticadoDto(usuario, email)
        );
    }

    private void validarDadosEntrada(CadastroUsuarioCommand command) {
        if (command == null) {
            throw new BusinessRuleException("Dados de cadastro são obrigatórios.");
        }
        validarTamanhoSenha(command.senha());
        CpfValidator.validar(command.cpf());
    }

    private void validarInexistenciaDeDuplicidades(String cpfHash, String telefone, String email) {
        if (usuarioRepository.existsByCpfHash(cpfHash)
                || usuarioRepository.existsByTelefone(telefone)
                || credencialRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Não foi possível concluir o cadastro com os dados informados.");
        }
    }

    private Usuario criarUsuarioComPerfilPadrao(CadastroUsuarioCommand command, String cpfHash, String telefone, OffsetDateTime agora) {
        Usuario usuario = Usuario.criar(
                command.nomeCompleto(),
                command.cpf(),
                cpfHash,
                telefone,
                command.genero(),
                command.racaEtnia(),
                command.orientacaoSexual(),
                agora
        );

        Perfil perfilPadrao = perfilRepository.findByNome(ROLE_USUARIO)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil padrão ROLE_USUARIO não encontrado."));
        usuario.adicionarPerfil(perfilPadrao, agora, usuario.getId());
        return usuario;
    }

    private Credencial criarCredencial(Usuario usuario, String email, String senhaPlana, OffsetDateTime agora) {
        String hashSenha = passwordEncoder.encode(senhaPlana);
        return Credencial.criar(usuario, email, hashSenha, agora);
    }

    private void validarPayloadLogin(LoginCommand command) {
        if (command == null || command.email() == null || command.email().isBlank() || command.senha() == null) {
            throw new UnauthorizedException("Credenciais inválidas.");
        }
    }

    private Credencial buscarCredencialOuExecutarDummyVerification(String emailNormalizado, String senhaPlana) {
        return credencialRepository.findByEmailWithUsuarioAndPerfis(emailNormalizado)
                .orElseGet(() -> {
                    passwordEncoder.matches(senhaPlana, DUMMY_PASSWORD_HASH);
                    log.warn("Tentativa de autenticação falhou: credencial inexistente.");
                    throw new UnauthorizedException("Credenciais inválidas.");
                });
    }

    private void validarSenha(Credencial credencial, String senhaPlana, OffsetDateTime agora) {
        if (!passwordEncoder.matches(senhaPlana, credencial.getHashSenha())) {
            credencial.registrarFalhaLogin(
                    agora,
                    lockoutProperties.maxTentativas(),
                    lockoutProperties.minutosBloqueio(),
                    lockoutProperties.tentativasAposDesbloqueio()
            );
            credencialRepository.save(credencial);
            log.warn("Tentativa de autenticação falhou: usuarioId={}, tentativasFalhas={}",
                    credencial.getUsuario().getId(), credencial.getTentativasFalhas());
            credencial.verificarBloqueio(agora);
            throw new UnauthorizedException("Credenciais inválidas.");
        }
    }

    private void registrarSucesso(Credencial credencial, OffsetDateTime agora) {
        credencial.registrarSucessoLogin(agora);
        credencialRepository.save(credencial);
    }

    private void validarTamanhoSenha(String senha) {
        if (senha == null || senha.length() < 8 || senha.length() > 64) {
            throw new BusinessRuleException("A senha deve conter entre 8 e 64 caracteres.");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("E-mail é obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
