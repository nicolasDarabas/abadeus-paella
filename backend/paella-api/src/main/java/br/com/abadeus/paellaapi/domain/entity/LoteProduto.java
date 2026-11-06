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

/** Preco e quantidade de um produto dentro de um lote. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lote_produto")
public class LoteProduto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "preco_lote", nullable = false)
    private BigDecimal precoLote;

    /** Quantidade disponivel neste lote. Nulo = sem limite. */
    private Integer quantidade;
}
