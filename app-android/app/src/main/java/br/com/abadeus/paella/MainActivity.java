package br.com.abadeus.paella;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.Map;

import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.ui.admin.AdminActivity;
import br.com.abadeus.paella.ui.admin.VoluntarioActivity;
import br.com.abadeus.paella.ui.auth.LoginActivity;
import br.com.abadeus.paella.ui.comprador.CompradorActivity;

/**
 * SPLASH: mostra a marca, verifica se o servidor esta no ar
 * e abre a tela certa (login, visao do comprador ou visao do administrador).
 */
public class MainActivity extends BaseActivity {

    private TextView status;
    private boolean saiu = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.splashStatus);

        findViewById(R.id.splashLogo).animate().scaleX(1.06f).scaleY(1.06f).setDuration(700).withEndAction(
                () -> findViewById(R.id.splashLogo).animate().scaleX(1f).scaleY(1f).setDuration(500).start()).start();

        new Handler(Looper.getMainLooper()).postDelayed(this::verificarServidor, 900);
    }

    private void verificarServidor() {
        ApiClient.get(this).status().enqueue(new Resposta<Map<String, Object>>(this) {
            @Override
            protected void sucesso(Map<String, Object> corpo) {
                status.setText("Servidor online");
                seguir();
            }

            @Override
            protected void falha(int codigo, String mensagem) {
                seguir();
            }

            @Override
            protected void semConexao() {
                status.setText("Servidor offline · usando dados salvos");
                new Handler(Looper.getMainLooper()).postDelayed(() -> seguir(), 1200);
            }
        });
    }

    private void seguir() {
        if (saiu) {
            return;
        }
        saiu = true;
        Class<?> destino;
        if (!Sessao.logado(this)) {
            destino = LoginActivity.class;
        } else if (Sessao.isAdministrador(this)) {
            destino = AdminActivity.class;
        } else if (Sessao.isVoluntario(this)) {
            destino = VoluntarioActivity.class;
        } else {
            destino = CompradorActivity.class;
        }
        startActivity(new Intent(this, destino));
        finish();
    }

    /** Abre a visao correta para o perfil logado, limpando a pilha de telas. */
    public static Intent telaInicial(android.content.Context c) {
        Class<?> tela = Sessao.isAdministrador(c) ? AdminActivity.class
                : Sessao.isVoluntario(c) ? VoluntarioActivity.class : CompradorActivity.class;
        Intent i = new Intent(c, tela);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return i;
    }
}
