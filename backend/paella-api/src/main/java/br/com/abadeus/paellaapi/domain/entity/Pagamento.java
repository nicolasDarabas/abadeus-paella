package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.MetodoPagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPagamento;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pagamento")
public class Pagamento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPagamento metodo;

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    /** Identificador no provedor de pagamento (simulado neste projeto). */
    @Column(name = "id_externo")
    private String idExterno;

    /** Codigo "copia e cola" do Pix (simulado). */
    @Column(name = "codigo_pix")
    private String codigoPix;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "confirmado_por")
    private String confirmadoPor;
}
