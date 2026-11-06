package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.OrigemPedido;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedido")
public class Pedido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemPedido origem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /** Codigo gravado no QR Code. So existe depois que o pedido e pago. */
    @Column(name = "codigo_retirada", unique = true)
    private String codigoRetirada;

    /** Ate quando o estoque fica reservado aguardando pagamento. */
    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    private String observacao;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> itens = new ArrayList<>();

    public void adicionarItem(PedidoItem item) {
        item.setPedido(this);
        itens.add(item);
    }
}
