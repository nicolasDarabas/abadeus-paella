package br.com.abadeus.paellaapi.security;

import br.com.abadeus.paellaapi.domain.enums.PerfilNome;

/** Dados do usuario logado, extraidos do token JWT. */
public record UsuarioAutenticado(Long id, String login, PerfilNome perfil) {

    public boolean isAdministrador() {
        return perfil == PerfilNome.ADMINISTRADOR;
    }

    public boolean isVoluntario() {
        return perfil == PerfilNome.VOLUNTARIO;
    }
}
