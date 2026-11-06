package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.TipoNotificacao;
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

/**
 * Aviso enviado ao comprador. Neste projeto o envio de e-mail e SIMULADO:
 * a mensagem fica registrada aqui e aparece no console da API.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notificacao")
public class Notificacao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo;

    @Column(nullable = false)
    private String canal = "EMAIL";

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String assunto;

    @Column(nullable = false, length = 2000)
    private String mensagem;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;

    @Column(nullable = false)
    private String status = "SIMULADO";
}
