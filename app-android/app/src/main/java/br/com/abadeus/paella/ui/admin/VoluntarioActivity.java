package br.com.abadeus.paella.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.ui.auth.LoginActivity;

/**
 * VISAO DO VOLUNTARIO (equipe de entrega): acesso somente a validacao de retirada,
 * sem dados financeiros nem cadastros.
 */
public class VoluntarioActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voluntario);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Retirada");
        toolbar.setSubtitle("Olá, " + Sessao.getPrimeiroNome(this) + " · leia o QR Code e entregue a paella");
        toolbar.inflateMenu(R.menu.menu_voluntario);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.acao_sair) {
                sair();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.conteudo, new RetiradaFragment())
                    .commit();
        }
    }

    private void sair() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sair da conta?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sair", (d, w) -> {
                    Sessao.sair(this);
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .show();
    }
}
