package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.AuthDtos.LoginRequest;
import br.com.abadeus.paellaapi.dto.AuthDtos.RegistroRequest;
import br.com.abadeus.paellaapi.dto.AuthDtos.TokenResponse;
import br.com.abadeus.paellaapi.dto.AuthDtos.UsuarioResponse;
import br.com.abadeus.paellaapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Botao ENTRAR: aceita login (ADMINISTRADOR) ou e-mail. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Botao REGISTRAR: cria um usuario com perfil COMPRADOR. */
    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    /** Dados do usuario logado: o app usa o "perfil" para abrir a visao certa. */
    @GetMapping("/me")
    public UsuarioResponse me() {
        return authService.usuarioLogado();
    }
}
