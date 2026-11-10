package br.com.lume.api.catalogo.domain;

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
@Table(name = "categorias_estabelecimentos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoriaEstabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    public static CategoriaEstabelecimento criar(
            Integer id,
            String codigo,
            String nome,
            boolean ativo
    ) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessRuleException("Código da categoria de estabelecimento é obrigatório.");
        }
        String codigoNormalizado = codigo.trim();
        if (codigoNormalizado.length() > 50) {
            throw new BusinessRuleException("Código da categoria de estabelecimento deve ter no máximo 50 caracteres.");
        }

        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome da categoria de estabelecimento é obrigatório.");
        }
        String nomeNormalizado = nome.trim();
        if (nomeNormalizado.length() > 100) {
            throw new BusinessRuleException("Nome da categoria de estabelecimento deve ter no máximo 100 caracteres.");
        }

        CategoriaEstabelecimento categoria = new CategoriaEstabelecimento();
        categoria.id = id;
        categoria.codigo = codigoNormalizado;
        categoria.nome = nomeNormalizado;
        categoria.ativo = ativo;
        return categoria;
    }

    public static CategoriaEstabelecimento criar(String codigo, String nome, boolean ativo) {
        return criar(null, codigo, nome, ativo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoriaEstabelecimento that)) {
            return false;
        }
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}