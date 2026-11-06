package br.com.abadeus.paellaapi.dto;

import br.com.abadeus.paellaapi.domain.entity.Usuario;
import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** "login" aceita o login (ex.: ADMINISTRADOR) ou o e-mail. */
    public record LoginRequest(
            @NotBlank(message = "Informe o login ou e-mail") String login,
            @NotBlank(message = "Informe a senha") String senha) {
    }

    public record RegistroRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") String email,
            @Size(max = 20) String telefone,
            @NotBlank(message = "Informe a senha") @Size(min = 6, max = 60, message = "A senha deve ter de 6 a 60 caracteres") String senha) {
    }

    public record TokenResponse(String token, String tipo, long expiraEmSegundos, UsuarioResponse usuario) {
    }

    public record UsuarioResponse(Long id, String nome, String login, String email, String telefone,
                                  PerfilNome perfil, boolean ativo) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getLogin(), u.getEmail(), u.getTelefone(),
                    u.getPerfil().getNome(), u.isAtivo());
        }
    }

    public record AtivoRequest(boolean ativo) {
    }

    /** Cadastro de usuario pelo administrador (ex.: voluntario da retirada). */
    public record NovoUsuarioRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") String email,
            @Size(max = 20) String telefone,
            @NotBlank(message = "Informe a senha") @Size(min = 6, max = 60, message = "A senha deve ter de 6 a 60 caracteres") String senha,
            PerfilNome perfil) {
    }
}
