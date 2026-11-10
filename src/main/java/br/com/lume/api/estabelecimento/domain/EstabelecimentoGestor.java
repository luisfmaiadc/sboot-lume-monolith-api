package br.com.lume.api.estabelecimento.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estabelecimentos_gestores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EstabelecimentoGestor {

    @EmbeddedId
    private EstabelecimentoGestorId id;

    @Column(name = "cargo", nullable = false, length = 50)
    private String cargo;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "vinculado_em", nullable = false, updatable = false)
    private OffsetDateTime vinculadoEm;

    @Column(name = "vinculado_por", updatable = false)
    private UUID vinculadoPor;

    public static EstabelecimentoGestor vincular(
            UUID estabelecimentoId,
            UUID usuarioId,
            String cargo,
            UUID vinculadoPor,
            OffsetDateTime agora
    ) {
        if (estabelecimentoId == null) {
            throw new BusinessRuleException("Identificador do estabelecimento é obrigatório para vínculo de gestor.");
        }
        if (usuarioId == null) {
            throw new BusinessRuleException("Identificador do usuário é obrigatório para vínculo de gestor.");
        }
        if (cargo == null || cargo.isBlank()) {
            throw new BusinessRuleException("Cargo do gestor é obrigatório.");
        }
        String cargoTrim = cargo.trim();
        if (cargoTrim.length() > 50) {
            throw new BusinessRuleException("Cargo do gestor deve ter no máximo 50 caracteres.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para vínculo de gestor.");
        }

        EstabelecimentoGestor gestor = new EstabelecimentoGestor();
        gestor.id = new EstabelecimentoGestorId(estabelecimentoId, usuarioId);
        gestor.cargo = cargoTrim;
        gestor.ativo = true;
        gestor.vinculadoEm = agora;
        gestor.vinculadoPor = vinculadoPor;
        return gestor;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getEstabelecimentoId() {
        return id != null ? id.getEstabelecimentoId() : null;
    }

    public UUID getUsuarioId() {
        return id != null ? id.getUsuarioId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EstabelecimentoGestor that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
