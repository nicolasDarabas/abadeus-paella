package br.com.abadeus.paellaapi.domain.entity;

import br.com.abadeus.paellaapi.security.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Campos comuns a todas as tabelas: id e auditoria
 * (criado_em, atualizado_em, criado_por, atualizado_por).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "criado_por", updatable = false)
    private String criadoPor;

    @Column(name = "atualizado_por")
    private String atualizadoPor;

    @PrePersist
    protected void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        String usuario = SecurityUtils.loginAtualOuSistema();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        this.criadoPor = usuario;
        this.atualizadoPor = usuario;
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
        this.atualizadoPor = SecurityUtils.loginAtualOuSistema();
    }
}
