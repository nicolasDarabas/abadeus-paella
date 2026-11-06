package br.com.abadeus.paellaapi.security;

import br.com.abadeus.paellaapi.domain.entity.Usuario;
import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gera e valida tokens JWT assinados com HMAC-SHA256 (HS256),
 * usando apenas a biblioteca padrao do Java.
 */
@Service
public class JwtService {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    private final byte[] segredo;
    private final long expiracaoSegundos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiracao-horas:12}") long expiracaoHoras) {
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalStateException("app.jwt.secret precisa ter pelo menos 32 caracteres");
        }
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        this.expiracaoSegundos = expiracaoHoras * 3600;
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }

    public String gerarToken(Usuario usuario) {
        long agora = Instant.now().getEpochSecond();
        String payload = "{"
                + "\"sub\":\"" + usuario.getId() + "\","
                + "\"login\":\"" + escapar(usuario.getLogin()) + "\","
                + "\"perfil\":\"" + usuario.getPerfil().getNome().name() + "\","
                + "\"iat\":" + agora + ","
                + "\"exp\":" + (agora + expiracaoSegundos)
                + "}";
        String cabecalho = B64.encodeToString(HEADER.getBytes(StandardCharsets.UTF_8));
        String corpo = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String conteudo = cabecalho + "." + corpo;
        return conteudo + "." + assinar(conteudo);
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return Optional.empty();
            }
            String esperado = assinar(partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                    partes[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            String payload = new String(B64_DEC.decode(partes[1]), StandardCharsets.UTF_8);
            long exp = Long.parseLong(numero(payload, "exp"));
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }
            Long id = Long.valueOf(texto(payload, "sub"));
            String login = texto(payload, "login").replace("\\\"", "\"").replace("\\\\", "\\");
            PerfilNome perfil = PerfilNome.valueOf(texto(payload, "perfil"));
            return Optional.of(new UsuarioAutenticado(id, login, perfil));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return B64.encodeToString(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar token", e);
        }
    }

    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String texto(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Campo ausente: " + campo);
        }
        return m.group(1);
    }

    private static String numero(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":(\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Campo ausente: " + campo);
        }
        return m.group(1);
    }
}
