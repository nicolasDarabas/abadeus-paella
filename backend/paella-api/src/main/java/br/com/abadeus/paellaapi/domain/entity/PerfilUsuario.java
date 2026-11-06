package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PerfilNome nome;

    private String descricao;

    public PerfilUsuario(PerfilNome nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}
