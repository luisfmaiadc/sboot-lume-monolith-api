package br.com.lume.api.identidade.repository;

import br.com.lume.api.identidade.domain.Usuario;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByCpfHash(String cpfHash);

    boolean existsByTelefone(String telefone);

    boolean existsByTelefoneAndIdNot(String telefone, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.perfis up LEFT JOIN FETCH up.perfil WHERE u.id = :id")
    Optional<Usuario> findByIdWithPerfis(@Param("id") UUID id);
}