package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.PerfilUsuario;
import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilUsuarioRepository extends JpaRepository<PerfilUsuario, Long> {
    Optional<PerfilUsuario> findByNome(PerfilNome nome);
}
