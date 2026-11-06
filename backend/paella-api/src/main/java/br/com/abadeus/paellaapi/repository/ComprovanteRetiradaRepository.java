package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.ComprovanteRetirada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprovanteRetiradaRepository extends JpaRepository<ComprovanteRetirada, Long> {
    Optional<ComprovanteRetirada> findByRetiradaId(Long retiradaId);
}
