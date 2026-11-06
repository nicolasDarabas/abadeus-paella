package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedido_status_historico")
public class PedidoStatusHistorico extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusPedido statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusPedido statusNovo;

    private String motivo;

    @Column(name = "alterado_por")
    private String alteradoPor;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;
}
