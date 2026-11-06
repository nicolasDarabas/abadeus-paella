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
@Table(name = "produto")
public class Produto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    /** Preco padrao, usado quando nao existe lote ativo para o produto. */
    @Column(nullable = false)
    private BigDecimal preco;

    /** Quantidade maxima vendida. Nulo = sem limite. */
    @Column(name = "limite_estoque")
    private Integer limiteEstoque;

    @Column(nullable = false)
    private boolean ativo = true;
}
