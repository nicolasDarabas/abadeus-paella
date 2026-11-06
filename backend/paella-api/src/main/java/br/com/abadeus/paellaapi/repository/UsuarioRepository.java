package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("select u from Usuario u where lower(u.login) = lower(:valor) or lower(u.email) = lower(:valor)")
    Optional<Usuario> findByLoginOuEmail(@Param("valor") String valor);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByLoginIgnoreCase(String login);
}
