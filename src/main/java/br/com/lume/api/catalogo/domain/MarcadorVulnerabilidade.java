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
@Table(name = "marcadores_vulnerabilidade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarcadorVulnerabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    public static MarcadorVulnerabilidade criar(
            Integer id,
            String codigo,
            String nome,
            String descricao,
            boolean ativo
    ) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessRuleException("Código do marcador de vulnerabilidade é obrigatório.");
        }
        String codigoNormalizado = codigo.trim();
        if (codigoNormalizado.length() > 50) {
            throw new BusinessRuleException("Código do marcador de vulnerabilidade deve ter no máximo 50 caracteres.");
        }

        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome do marcador de vulnerabilidade é obrigatório.");
        }
        String nomeNormalizado = nome.trim();
        if (nomeNormalizado.length() > 100) {
            throw new BusinessRuleException("Nome do marcador de vulnerabilidade deve ter no máximo 100 caracteres.");
        }

        String descricaoNormalizada = descricao != null ? descricao.trim() : null;
        if (descricaoNormalizada != null && descricaoNormalizada.length() > 255) {
            throw new BusinessRuleException("Descrição do marcador de vulnerabilidade deve ter no máximo 255 caracteres.");
        }

        MarcadorVulnerabilidade marcador = new MarcadorVulnerabilidade();
        marcador.id = id;
        marcador.codigo = codigoNormalizado;
        marcador.nome = nomeNormalizado;
        marcador.descricao = descricaoNormalizada;
        marcador.ativo = ativo;
        return marcador;
    }

    public static MarcadorVulnerabilidade criar(
            String codigo,
            String nome,
            String descricao,
            boolean ativo
    ) {
        return criar(null, codigo, nome, descricao, ativo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MarcadorVulnerabilidade that)) {
            return false;
        }
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
