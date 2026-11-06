package br.com.abadeus.paella.ui.comprador;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.R;

/** VISAO DO COMPRADOR: Cardapio, Meus pedidos e Perfil. */
public class CompradorActivity extends BaseActivity {

    public static final String EXTRA_ABA = "aba";

    private BottomNavigationView navegacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprador);

        navegacao = findViewById(R.id.navegacao);
        navegacao.setOnItemSelectedListener(item -> {
            mostrar(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            navegacao.setSelectedItemId(getIntent().getIntExtra(EXTRA_ABA, R.id.aba_cardapio));
        }
    }

    public void irPara(int abaId) {
        navegacao.setSelectedItemId(abaId);
    }

    private void mostrar(int id) {
        Fragment f;
        if (id == R.id.aba_pedidos) {
            f = new MeusPedidosFragment();
        } else if (id == R.id.aba_perfil) {
            f = new PerfilFragment();
        } else {
            f = new CardapioFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.conteudo, f).commit();
    }
}
