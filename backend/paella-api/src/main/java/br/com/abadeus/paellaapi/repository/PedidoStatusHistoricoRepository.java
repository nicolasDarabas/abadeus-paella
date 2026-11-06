package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.PedidoStatusHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoStatusHistoricoRepository extends JpaRepository<PedidoStatusHistorico, Long> {
    List<PedidoStatusHistorico> findByPedidoIdOrderByAlteradoEmAsc(Long pedidoId);
}
