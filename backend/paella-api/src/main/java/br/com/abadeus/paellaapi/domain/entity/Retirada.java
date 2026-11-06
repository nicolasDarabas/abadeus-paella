package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.MetodoValidacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "retirada")
public class Retirada extends BaseEntity {

    /** UNIQUE no banco: um pedido so pode ser retirado uma vez. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "validado_por", nullable = false)
    private String validadoPor;

    @Column(name = "validado_em", nullable = false)
    private LocalDateTime validadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_validacao", nullable = false)
    private MetodoValidacao metodoValidacao;

    @Column(name = "sincronizado_offline", nullable = false)
    private boolean sincronizadoOffline = false;
}
