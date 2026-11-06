package br.com.abadeus.paella.data;

import android.content.Context;
import android.content.SharedPreferences;

import br.com.abadeus.paella.data.api.Modelos;

/** Dados do usuario logado e preferencias, guardados no aparelho. */
public final class Sessao {

    /** 10.0.2.2 = "localhost" do computador, visto de dentro do emulador Android. */
    public static final String SERVIDOR_PADRAO = "http://10.0.2.2:8080/";

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String COMPRADOR = "COMPRADOR";
    public static final String VOLUNTARIO = "VOLUNTARIO";

    private static final String ARQUIVO = "abadeus_sessao";

    private Sessao() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public static void salvarLogin(Context c, Modelos.TokenResponse t) {
        prefs(c).edit()
                .putString("token", t.token)
                .putLong("usuarioId", t.usuario.id)
                .putString("nome", t.usuario.nome)
                .putString("email", t.usuario.email)
                .putString("perfil", t.usuario.perfil)
                .apply();
    }

    public static void sair(Context c) {
        String servidor = getServidor(c);
        prefs(c).edit().clear().putString("servidor", servidor).apply();
        Carrinho.get().limpar();
    }

    public static boolean logado(Context c) {
        return getToken(c) != null;
    }

    public static String getToken(Context c) {
        return prefs(c).getString("token", null);
    }

    public static String getNome(Context c) {
        return prefs(c).getString("nome", "");
    }

    public static String getPrimeiroNome(Context c) {
        String nome = getNome(c).trim();
        int espaco = nome.indexOf(' ');
        return espaco > 0 ? nome.substring(0, espaco) : nome;
    }

    public static String getEmail(Context c) {
        return prefs(c).getString("email", "");
    }

    public static String getPerfil(Context c) {
        return prefs(c).getString("perfil", "");
    }

    public static boolean isAdministrador(Context c) {
        return ADMINISTRADOR.equals(getPerfil(c));
    }

    public static boolean isVoluntario(Context c) {
        return VOLUNTARIO.equals(getPerfil(c));
    }

    // ---------- Servidor ----------

    public static String getServidor(Context c) {
        return prefs(c).getString("servidor", SERVIDOR_PADRAO);
    }

    public static void setServidor(Context c, String url) {
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        if (!u.endsWith("/")) {
            u = u + "/";
        }
        prefs(c).edit().putString("servidor", u).apply();
    }

    // ---------- Administrador ----------

    public static long getEventoAdmin(Context c) {
        return prefs(c).getLong("eventoAdmin", -1);
    }

    public static void setEventoAdmin(Context c, long eventoId) {
        prefs(c).edit().putLong("eventoAdmin", eventoId).apply();
    }

    public static boolean isModoOffline(Context c) {
        return prefs(c).getBoolean("modoOffline", false);
    }

    public static void setModoOffline(Context c, boolean ativo) {
        prefs(c).edit().putBoolean("modoOffline", ativo).apply();
    }
}
