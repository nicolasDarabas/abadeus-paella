package br.com.abadeus.paellaapi.security;

import br.com.abadeus.paellaapi.exception.NaoAutenticadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UsuarioAutenticado> usuarioAtualOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static UsuarioAutenticado usuarioAtual() {
        return usuarioAtualOpcional().orElseThrow(NaoAutenticadoException::new);
    }

    /** Usado na auditoria (criado_por / atualizado_por). */
    public static String loginAtualOuSistema() {
        return usuarioAtualOpcional().map(UsuarioAutenticado::login).orElse("sistema");
    }
}
