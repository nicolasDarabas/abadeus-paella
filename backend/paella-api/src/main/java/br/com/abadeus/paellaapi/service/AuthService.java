package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.PerfilUsuario;
import br.com.abadeus.paellaapi.domain.entity.Usuario;
import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import br.com.abadeus.paellaapi.dto.AuthDtos.LoginRequest;
import br.com.abadeus.paellaapi.dto.AuthDtos.RegistroRequest;
import br.com.abadeus.paellaapi.dto.AuthDtos.TokenResponse;
import br.com.abadeus.paellaapi.dto.AuthDtos.UsuarioResponse;
import br.com.abadeus.paellaapi.exception.ConflitoException;
import br.com.abadeus.paellaapi.exception.NaoAutenticadoException;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.repository.PerfilUsuarioRepository;
import br.com.abadeus.paellaapi.repository.UsuarioRepository;
import br.com.abadeus.paellaapi.security.JwtService;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PerfilUsuarioRepository perfilRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByLoginOuEmail(req.login().trim())
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new NaoAutenticadoException("Login ou senha invalidos."));
        if (!usuario.isAtivo()) {
            throw new NaoAutenticadoException("Usuario desativado. Procure a organizacao.");
        }
        return gerarToken(usuario);
    }

    /** Cadastro publico: sempre cria um COMPRADOR. O login passa a ser o proprio e-mail. */
    @Transactional
    public TokenResponse registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email) || usuarioRepository.existsByLoginIgnoreCase(email)) {
            throw new ConflitoException("Ja existe um cadastro com este e-mail.");
        }
        PerfilUsuario perfil = perfilRepository.findByNome(PerfilNome.COMPRADOR)
                .orElseThrow(() -> new IllegalStateException("Perfil COMPRADOR nao cadastrado"));

        Usuario u = new Usuario();
        u.setPerfil(perfil);
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setLogin(email);
        u.setTelefone(req.telefone());
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setAtivo(true);
        usuarioRepository.save(u);
        return gerarToken(u);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioLogado() {
        Long id = SecurityUtils.usuarioAtual().id();
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new NaoEncontradoException("Usuario", id));
    }

    private TokenResponse gerarToken(Usuario u) {
        return new TokenResponse(jwtService.gerarToken(u), "Bearer", jwtService.getExpiracaoSegundos(),
                UsuarioResponse.de(u));
    }
}
