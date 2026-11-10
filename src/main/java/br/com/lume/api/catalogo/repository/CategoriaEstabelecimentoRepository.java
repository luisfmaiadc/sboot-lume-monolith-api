package br.com.lume.api.catalogo.repository;

import br.com.lume.api.catalogo.domain.CategoriaEstabelecimento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaEstabelecimentoRepository extends JpaRepository<CategoriaEstabelecimento, Integer> {

    List<CategoriaEstabelecimento> findAllByAtivoTrueOrderByNomeAsc();

    Optional<CategoriaEstabelecimento> findByIdAndAtivoTrue(Integer id);
}