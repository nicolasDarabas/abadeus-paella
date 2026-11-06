package br.com.abadeus.paella;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base das telas: o Android 15+ desenha o app atras da barra de status,
 * entao aplicamos um espaco (padding) para o conteudo nao ficar escondido.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        View raiz = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, Math.max(barras.bottom, teclado.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
