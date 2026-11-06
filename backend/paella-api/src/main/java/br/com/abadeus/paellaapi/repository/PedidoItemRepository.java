package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.PedidoItem;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {

    /** Quantidade do produto ja comprometida (reservada, paga ou retirada). */
    @Query("""
            select coalesce(sum(i.quantidade), 0) from PedidoItem i
            where i.produto.id = :produtoId and i.pedido.status in :status
            """)
    Long somarQuantidadeProduto(@Param("produtoId") Long produtoId,
                                @Param("status") Collection<StatusPedido> status);

    /** Quantidade do produto ja comprometida dentro de um lote. */
    @Query("""
            select coalesce(sum(i.quantidade), 0) from PedidoItem i
            where i.produto.id = :produtoId and i.lote.id = :loteId and i.pedido.status in :status
            """)
    Long somarQuantidadeProdutoNoLote(@Param("produtoId") Long produtoId,
                                      @Param("loteId") Long loteId,
                                      @Param("status") Collection<StatusPedido> status);
}
