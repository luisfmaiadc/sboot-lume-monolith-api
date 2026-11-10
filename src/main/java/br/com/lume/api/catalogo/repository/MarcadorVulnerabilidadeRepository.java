package br.com.lume.api.catalogo.repository;

import br.com.lume.api.catalogo.domain.MarcadorVulnerabilidade;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcadorVulnerabilidadeRepository extends JpaRepository<MarcadorVulnerabilidade, Integer> {

    List<MarcadorVulnerabilidade> findAllByAtivoTrueOrderByIdAsc();

    Optional<MarcadorVulnerabilidade> findByIdAndAtivoTrue(Integer id);
}