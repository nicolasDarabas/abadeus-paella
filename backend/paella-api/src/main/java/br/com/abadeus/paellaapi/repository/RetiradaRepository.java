package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Retirada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetiradaRepository extends JpaRepository<Retirada, Long> {
    boolean existsByPedidoId(Long pedidoId);

    Optional<Retirada> findByPedidoId(Long pedidoId);

    List<Retirada> findByPedidoEventoIdOrderByValidadoEmDesc(Long eventoId);
}
