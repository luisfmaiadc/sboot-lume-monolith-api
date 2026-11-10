package br.com.lume.api.identidade.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "perfis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, unique = true, length = 50)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    public static Perfil criar(Integer id, String nome, String descricao) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome do perfil é obrigatório.");
        }
        String nomeNormalizado = nome.trim();
        if (nomeNormalizado.length() > 50) {
            throw new BusinessRuleException("Nome do perfil deve ter no máximo 50 caracteres.");
        }

        Perfil perfil = new Perfil();
        perfil.id = id;
        perfil.nome = nomeNormalizado;
        perfil.descricao = descricao != null ? descricao.trim() : null;
        return perfil;
    }

    public static Perfil criar(String nome, String descricao) {
        return criar(null, nome, descricao);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Perfil that)) {
            return false;
        }
        return Objects.equals(nome, that.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nome);
    }
}
