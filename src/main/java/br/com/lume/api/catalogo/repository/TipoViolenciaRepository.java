package br.com.lume.api.catalogo.repository;

import br.com.lume.api.catalogo.domain.TipoViolencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoViolenciaRepository extends JpaRepository<TipoViolencia, Integer> {

    List<TipoViolencia> findAllByAtivoTrueOrderByOrdemExibicaoAscNomeAsc();

    Optional<TipoViolencia> findByIdAndAtivoTrue(Integer id);
}