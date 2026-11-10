package br.com.lume.api.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lume.api.identidade.command.AtualizacaoUsuarioCommand;
import br.com.lume.api.identidade.domain.Credencial;
import br.com.lume.api.identidade.domain.Perfil;
import br.com.lume.api.identidade.domain.Usuario;
import br.com.lume.api.identidade.dto.UsuarioDetalheDto;
import br.com.lume.api.identidade.dto.UsuarioResumoDto;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.identidade.repository.CredencialRepository;
import br.com.lume.api.identidade.repository.UsuarioRepository;
import br.com.lume.api.model.AtualizacaoUsuarioRequest;
import br.com.lume.api.model.Genero;
import br.com.lume.api.model.OrientacaoSexual;
import br.com.lume.api.model.RacaEtnia;
import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import br.com.lume.api.shared.exception.ResourceNotFoundException;
import br.com.lume.api.shared.security.AuthenticatedUserProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Testes Unitários")
class UsuarioServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);
    private static final String CPF_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CredencialRepository credencialRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private UsuarioMapper usuarioMapper;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioMapper = Mappers.getMapper(UsuarioMapper.class);
        usuarioService = new UsuarioService(
                usuarioRepository,
                credencialRepository,
                authenticatedUserProvider,
                FIXED_CLOCK,
                usuarioMapper
        );
    }

    @Nested
    @DisplayName("obterPerfilLogado()")
    class ObterPerfilLogado {

        @Test
        @DisplayName("Deve retornar UsuarioDetalheDto do usuário autenticado")
        void deveRetornarPerfilLogadoComSucesso() {
            Usuario usuario = criarUsuarioPadrao();
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(credencial));

            UsuarioDetalheDto response = usuarioService.obterPerfilLogado();

            assertThat(response.id()).isEqualTo(usuario.getId());
            assertThat(response.nomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(response.cpf()).isEqualTo("52998224725");
            assertThat(response.telefone()).isEqualTo("11987654321");
            assertThat(response.email()).isEqualTo("maria@exemplo.com");
            assertThat(response.genero()).isEqualTo(br.com.lume.api.identidade.domain.Genero.MULHER_CIS);
            assertThat(response.racaEtnia()).isEqualTo(br.com.lume.api.identidade.domain.RacaEtnia.PARDA);
            assertThat(response.orientacaoSexual()).isEqualTo(br.com.lume.api.identidade.domain.OrientacaoSexual.BISSEXUAL);
            assertThat(response.perfis()).containsExactly("ROLE_USUARIO");
            assertThat(response.criadoEm()).isEqualTo(AGORA);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando usuário ou credencial não forem encontrados")
        void deveLancarResourceNotFoundQuandoAusente() {
            Usuario usuario = criarUsuarioPadrao();
            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obterPerfilLogado())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuário não encontrado.");

            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obterPerfilLogado())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Credencial do usuário não encontrada.");
        }
    }

    @Nested
    @DisplayName("atualizarPerfilLogado(AtualizacaoUsuarioCommand)")
    class AtualizarPerfilLogado {

        @Test
        @DisplayName("Deve atualizar dados cadastrais do usuário logado quando não houver conflito de telefone")
        void deveAtualizarPerfilComSucesso() {
            Usuario usuario = criarUsuarioPadrao();
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);
            AtualizacaoUsuarioRequest request = new AtualizacaoUsuarioRequest()
                    .nomeCompleto("Maria da Silva Atualizada")
                    .telefone(" 11999990000 ")
                    .genero(Genero.MULHER_TRANS)
                    .racaEtnia(RacaEtnia.PRETA)
                    .orientacaoSexual(OrientacaoSexual.HOMOSSEXUAL);

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(credencial));
            when(usuarioRepository.existsByTelefoneAndIdNot("11999990000", usuario.getId())).thenReturn(false);

            UsuarioDetalheDto response = usuarioService.atualizarPerfilLogado(usuarioMapper.toCommand(request));

            verify(usuarioRepository).save(usuario);
            assertThat(response.nomeCompleto()).isEqualTo("Maria da Silva Atualizada");
            assertThat(response.telefone()).isEqualTo("11999990000");
            assertThat(response.genero()).isEqualTo(br.com.lume.api.identidade.domain.Genero.MULHER_TRANS);
            assertThat(response.racaEtnia()).isEqualTo(br.com.lume.api.identidade.domain.RacaEtnia.PRETA);
            assertThat(response.orientacaoSexual()).isEqualTo(br.com.lume.api.identidade.domain.OrientacaoSexual.HOMOSSEXUAL);
        }

        @Test
        @DisplayName("Deve atualizar apenas o nome e preservar gênero, raça e orientação existentes quando omitidos no payload")
        void devePreservarDadosDemograficosQuandoOmitidos() {
            Usuario usuario = criarUsuarioPadrao();
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);
            AtualizacaoUsuarioRequest request = new AtualizacaoUsuarioRequest()
                    .nomeCompleto("Luis Felipe Maia da Costa");

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(credencial));

            UsuarioDetalheDto response = usuarioService.atualizarPerfilLogado(usuarioMapper.toCommand(request));

            verify(usuarioRepository).save(usuario);
            assertThat(response.nomeCompleto()).isEqualTo("Luis Felipe Maia da Costa");
            assertThat(response.telefone()).isEqualTo("11987654321");
            assertThat(response.genero()).isEqualTo(br.com.lume.api.identidade.domain.Genero.MULHER_CIS);
            assertThat(response.racaEtnia()).isEqualTo(br.com.lume.api.identidade.domain.RacaEtnia.PARDA);
            assertThat(response.orientacaoSexual()).isEqualTo(br.com.lume.api.identidade.domain.OrientacaoSexual.BISSEXUAL);
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando telefone já pertencer a outro usuário")
        void deveLancarConflictExceptionQuandoTelefoneEmUsoPorOutroUsuario() {
            Usuario usuario = criarUsuarioPadrao();
            Credencial credencial = Credencial.criar(usuario, "maria@exemplo.com", "hash", AGORA);
            AtualizacaoUsuarioRequest request = new AtualizacaoUsuarioRequest().telefone("11999990000");

            when(authenticatedUserProvider.getUsuarioIdAutenticado()).thenReturn(usuario.getId());
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));
            when(credencialRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(credencial));
            when(usuarioRepository.existsByTelefoneAndIdNot("11999990000", usuario.getId())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.atualizarPerfilLogado(usuarioMapper.toCommand(request)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Já existe outro usuário cadastrado com o telefone informado.");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando request de atualização for nulo")
        void deveRejeitarRequestNulo() {
            assertThatThrownBy(() -> usuarioService.atualizarPerfilLogado((AtualizacaoUsuarioCommand) null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Dados de atualização são obrigatórios.");
        }
    }

    @Nested
    @DisplayName("buscarResumoPorId e usuarioPossuiPerfil (IdentidadeInternalFacade)")
    class FacadeMethods {

        @Test
        @DisplayName("Deve retornar Optional com UsuarioResumoDto quando usuário existir e vazio quando nulo ou inexistente")
        void deveBuscarResumoPorIdCorretamente() {
            Usuario usuario = criarUsuarioPadrao();
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));

            Optional<UsuarioResumoDto> resultado = usuarioService.buscarResumoPorId(usuario.getId());

            assertThat(resultado).isPresent();
            assertThat(resultado.get().id()).isEqualTo(usuario.getId());
            assertThat(resultado.get().nomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(resultado.get().ativo()).isTrue();
            assertThat(resultado.get().perfis()).containsExactly("ROLE_USUARIO");

            assertThat(usuarioService.buscarResumoPorId(null)).isEmpty();
            UUID idInexistente = UUID.randomUUID();
            when(usuarioRepository.findByIdWithPerfis(idInexistente)).thenReturn(Optional.empty());
            assertThat(usuarioService.buscarResumoPorId(idInexistente)).isEmpty();

            UsuarioResumoDto dtoPerfisNulos = new UsuarioResumoDto(usuario.getId(), "Nome", true, null);
            assertThat(dtoPerfisNulos.perfis()).isEmpty();
        }

        @Test
        @DisplayName("Deve verificar se usuário está ativo e possui o perfil solicitado")
        void deveVerificarSeUsuarioPossuiPerfil() {
            Usuario usuario = criarUsuarioPadrao();
            when(usuarioRepository.findByIdWithPerfis(usuario.getId())).thenReturn(Optional.of(usuario));

            assertThat(usuarioService.usuarioPossuiPerfil(usuario.getId(), "ROLE_USUARIO")).isTrue();
            assertThat(usuarioService.usuarioPossuiPerfil(usuario.getId(), "ROLE_GESTOR")).isFalse();
            assertThat(usuarioService.usuarioPossuiPerfil(null, "ROLE_USUARIO")).isFalse();
            assertThat(usuarioService.usuarioPossuiPerfil(usuario.getId(), null)).isFalse();
            assertThat(usuarioService.usuarioPossuiPerfil(usuario.getId(), "   ")).isFalse();

            ReflectionTestUtils.setField(usuario, "ativo", false);
            assertThat(usuarioService.usuarioPossuiPerfil(usuario.getId(), "ROLE_USUARIO")).isFalse();
        }

        @Test
        @DisplayName("Deve adquirir lock pessimista para usuário existente e lançar exceção quando nulo ou inexistente")
        void deveAdquirirLockParaUsuarioCorretamente() {
            Usuario usuario = criarUsuarioPadrao();
            when(usuarioRepository.findByIdWithLock(usuario.getId())).thenReturn(Optional.of(usuario));

            usuarioService.adquirirLockParaUsuario(usuario.getId());
            verify(usuarioRepository).findByIdWithLock(usuario.getId());

            assertThatThrownBy(() -> usuarioService.adquirirLockParaUsuario(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuário não encontrado.");

            UUID idInexistente = UUID.randomUUID();
            when(usuarioRepository.findByIdWithLock(idInexistente)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> usuarioService.adquirirLockParaUsuario(idInexistente))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuário não encontrado.");
        }
    }

    private Usuario criarUsuarioPadrao() {
        Usuario usuario = Usuario.criar(
                "Maria da Silva",
                "52998224725",
                CPF_HASH,
                "11987654321",
                br.com.lume.api.identidade.domain.Genero.MULHER_CIS,
                br.com.lume.api.identidade.domain.RacaEtnia.PARDA,
                br.com.lume.api.identidade.domain.OrientacaoSexual.BISSEXUAL,
                AGORA
        );
        Perfil perfil = Perfil.criar(1, "ROLE_USUARIO", "Cidadão");
        usuario.adicionarPerfil(perfil, AGORA, usuario.getId());
        return usuario;
    }
}
