package br.com.abadeus.paella.ui.admin;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.R;

/** VISAO DO ADMINISTRADOR: Painel, Pedidos e Retirada (leitor de QR Code). */
public class AdminActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        BottomNavigationView navegacao = findViewById(R.id.navegacao);
        navegacao.setOnItemSelectedListener(item -> {
            mostrar(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            navegacao.setSelectedItemId(R.id.aba_painel);
        }
    }

    private void mostrar(int id) {
        Fragment f;
        if (id == R.id.aba_pedidos_admin) {
            f = new PedidosAdminFragment();
        } else if (id == R.id.aba_retirada) {
            f = new RetiradaFragment();
        } else {
            f = new PainelFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.conteudo, f).commit();
    }
}
