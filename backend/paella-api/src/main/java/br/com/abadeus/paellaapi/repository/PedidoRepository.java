package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    boolean existsByNumero(String numero);

    Optional<Pedido> findByCodigoRetirada(String codigoRetirada);

    Optional<Pedido> findByNumero(String numero);

    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    List<Pedido> findByEventoIdOrderByCriadoEmDesc(Long eventoId);

    List<Pedido> findByEventoIdAndStatusOrderByCriadoEmDesc(Long eventoId, StatusPedido status);

    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);

    List<Pedido> findAllByOrderByCriadoEmDesc();

    List<Pedido> findByEventoIdAndStatus(Long eventoId, StatusPedido status);

    List<Pedido> findByStatusAndExpiraEmBefore(StatusPedido status, LocalDateTime momento);

    long countByEventoIdAndStatus(Long eventoId, StatusPedido status);

    @Query("select coalesce(sum(p.valorTotal), 0) from Pedido p where p.evento.id = :eventoId and p.status in :status")
    java.math.BigDecimal somarValor(@Param("eventoId") Long eventoId, @Param("status") Collection<StatusPedido> status);
}
