package br.com.lume.api.identidade.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios_perfis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsuarioPerfil {

    @EmbeddedId
    private UsuarioPerfilId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("perfilId")
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @Column(name = "atribuido_em", nullable = false)
    private OffsetDateTime atribuidoEm;

    @Column(name = "atribuido_por")
    private UUID atribuidoPor;

    public static UsuarioPerfil vincular(
            Usuario usuario,
            Perfil perfil,
            OffsetDateTime agora,
            UUID atribuidoPor
    ) {
        if (usuario == null) {
            throw new BusinessRuleException("Usuário é obrigatório para vínculo de perfil.");
        }
        if (perfil == null) {
            throw new BusinessRuleException("Perfil é obrigatório para vínculo ao usuário.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para vínculo de perfil.");
        }

        UsuarioPerfil vinculo = new UsuarioPerfil();
        vinculo.id = new UsuarioPerfilId(usuario.getId(), perfil.getId());
        vinculo.usuario = usuario;
        vinculo.perfil = perfil;
        vinculo.atribuidoEm = agora;
        vinculo.atribuidoPor = atribuidoPor;
        return vinculo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsuarioPerfil that)) {
            return false;
        }
        UUID thisUsuarioId = usuario != null ? usuario.getId() : (id != null ? id.getUsuarioId() : null);
        UUID thatUsuarioId = that.usuario != null ? that.usuario.getId() : (that.id != null ? that.id.getUsuarioId() : null);
        String thisPerfilNome = perfil != null ? perfil.getNome() : null;
        String thatPerfilNome = that.perfil != null ? that.perfil.getNome() : null;
        return Objects.equals(thisUsuarioId, thatUsuarioId)
                && Objects.equals(thisPerfilNome, thatPerfilNome);
    }

    @Override
    public int hashCode() {
        UUID thisUsuarioId = usuario != null ? usuario.getId() : (id != null ? id.getUsuarioId() : null);
        String thisPerfilNome = perfil != null ? perfil.getNome() : null;
        return Objects.hash(thisUsuarioId, thisPerfilNome);
    }
}
