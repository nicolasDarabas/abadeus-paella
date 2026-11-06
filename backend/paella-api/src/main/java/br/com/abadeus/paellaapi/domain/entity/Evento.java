package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.StatusEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evento")
public class Evento extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Column(name = "local", nullable = false)
    private String local;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;

    @Column(name = "retirada_inicio")
    private LocalDateTime retiradaInicio;

    @Column(name = "retirada_fim")
    private LocalDateTime retiradaFim;

    /** Telefone, WhatsApp ou e-mail para duvidas. */
    private String contato;

    /** Regras e instrucoes da retirada (levar o QR Code, embalagem etc.). */
    private String regras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEvento status = StatusEvento.RASCUNHO;
}
