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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false, unique = true)
    private String email;

    private String telefone;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private boolean ativo = true;
}
