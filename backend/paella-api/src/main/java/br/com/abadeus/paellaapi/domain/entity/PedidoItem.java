package br.com.abadeus.paellaapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedido_item")
public class PedidoItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /** Lote usado para definir o preco (pode ser nulo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private BigDecimal subtotal;
}
