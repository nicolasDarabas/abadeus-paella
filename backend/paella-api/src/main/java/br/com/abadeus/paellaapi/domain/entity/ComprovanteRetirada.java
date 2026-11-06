package br.com.abadeus.paellaapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "comprovante_retirada")
public class ComprovanteRetirada extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retirada_id", nullable = false, unique = true)
    private Retirada retirada;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(name = "emitido_em", nullable = false)
    private LocalDateTime emitidoEm;
}
