package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Pagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoIdOrderByCriadoEmDesc(Long pedidoId);

    List<Pagamento> findByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);
}
