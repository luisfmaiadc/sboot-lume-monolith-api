package br.com.lume.api.identidade.repository;

import br.com.lume.api.identidade.domain.Credencial;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CredencialRepository extends JpaRepository<Credencial, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credencial c JOIN FETCH c.usuario u LEFT JOIN FETCH u.perfis up LEFT JOIN FETCH up.perfil WHERE LOWER(c.email) = LOWER(:email)")
    Optional<Credencial> findByEmailWithUsuarioAndPerfis(@Param("email") String email);

    Optional<Credencial> findByUsuarioId(UUID usuarioId);
}