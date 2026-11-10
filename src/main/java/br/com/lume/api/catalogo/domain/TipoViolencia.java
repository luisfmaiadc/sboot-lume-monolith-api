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

@Entity
@Table(name = "tipos_violencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TipoViolencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "peso_gravidade", nullable = false)
    private int pesoGravidade;

    @Column(name = "ordem_exibicao")
    private Integer ordemExibicao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    public static TipoViolencia criar(
            Integer id,
            String codigo,
            String nome,
            int pesoGravidade,
            Integer ordemExibicao,
            boolean ativo
    ) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessRuleException("Código do tipo de violência é obrigatório.");
        }
        String codigoNormalizado = codigo.trim();
        if (codigoNormalizado.length() > 50) {
            throw new BusinessRuleException("Código do tipo de violência deve ter no máximo 50 caracteres.");
        }

        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome do tipo de violência é obrigatório.");
        }
        String nomeNormalizado = nome.trim();
        if (nomeNormalizado.length() > 100) {
            throw new BusinessRuleException("Nome do tipo de violência deve ter no máximo 100 caracteres.");
        }

        if (pesoGravidade < 1 || pesoGravidade > 10) {
            throw new BusinessRuleException("Peso de gravidade deve estar entre 1 e 10.");
        }

        TipoViolencia tipo = new TipoViolencia();
        tipo.id = id;
        tipo.codigo = codigoNormalizado;
        tipo.nome = nomeNormalizado;
        tipo.pesoGravidade = pesoGravidade;
        tipo.ordemExibicao = ordemExibicao;
        tipo.ativo = ativo;
        return tipo;
    }

    public static TipoViolencia criar(
            String codigo,
            String nome,
            int pesoGravidade,
            Integer ordemExibicao,
            boolean ativo
    ) {
        return criar(null, codigo, nome, pesoGravidade, ordemExibicao, ativo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TipoViolencia that)) {
            return false;
        }
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
