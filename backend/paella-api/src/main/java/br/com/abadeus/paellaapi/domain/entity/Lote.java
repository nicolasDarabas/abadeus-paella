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

import java.time.LocalDateTime;

/** Lote de vendas (ex.: 1o lote promocional, 2o lote...). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lote")
public class Lote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false)
    private String nome;

    @Column(name = "venda_inicio", nullable = false)
    private LocalDateTime vendaInicio;

    @Column(name = "venda_fim", nullable = false)
    private LocalDateTime vendaFim;

    @Column(nullable = false)
    private boolean ativo = true;

    public boolean vigenteEm(LocalDateTime momento) {
        return ativo && !momento.isBefore(vendaInicio) && !momento.isAfter(vendaFim);
    }
}
