package br.com.abadeus.paella.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.MainActivity;
import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;

/** Tela de LOGIN: logo, login/e-mail, senha, ENTRAR e REGISTRAR. */
public class LoginActivity extends BaseActivity {

    private TextInputLayout campoLogin;
    private TextInputLayout campoSenha;
    private TextInputEditText editLogin;
    private TextInputEditText editSenha;
    private MaterialButton botaoEntrar;
    private View progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        campoLogin = findViewById(R.id.campoLogin);
        campoSenha = findViewById(R.id.campoSenha);
        editLogin = findViewById(R.id.editLogin);
        editSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.botaoEntrar);
        progresso = findViewById(R.id.progressoLogin);

        botaoEntrar.setOnClickListener(v -> entrar());
        findViewById(R.id.botaoRegistrar).setOnClickListener(
                v -> startActivity(new Intent(this, RegistroActivity.class)));
        findViewById(R.id.botaoServidor).setOnClickListener(v -> configurarServidor(this));
        editSenha.setOnEditorActionListener((v, acao, e) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) {
                entrar();
                return true;
            }
            return false;
        });
    }

    private void entrar() {
        String login = texto(editLogin);
        String senha = texto(editSenha);
        campoLogin.setError(login.isEmpty() ? "Informe o login ou e-mail" : null);
        campoSenha.setError(senha.isEmpty() ? "Informe a senha" : null);
        if (login.isEmpty() || senha.isEmpty()) {
            return;
        }
        carregando(true);
        ApiClient.get(this).login(new Modelos.LoginRequest(login, senha))
                .enqueue(new Resposta<Modelos.TokenResponse>(this) {
                    @Override
                    protected void sucesso(Modelos.TokenResponse t) {
                        Sessao.salvarLogin(LoginActivity.this, t);
                        startActivity(MainActivity.telaInicial(LoginActivity.this));
                        finish();
                    }

                    @Override
                    protected void falha(int status, String mensagem) {
                        campoSenha.setError(mensagem);
                    }

                    @Override
                    protected void fim() {
                        carregando(false);
                    }
                });
    }

    private void carregando(boolean sim) {
        botaoEntrar.setEnabled(!sim);
        progresso.setVisibility(sim ? View.VISIBLE : View.INVISIBLE);
    }

    static String texto(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    /**
     * Endereco da API. Emulador: http://10.0.2.2:8080
     * Celular de verdade: http://IP-DO-COMPUTADOR:8080 (mesma rede Wi-Fi).
     */
    public static void configurarServidor(Context c) {
        EditText campo = new EditText(c);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campo.setText(Sessao.getServidor(c));
        FrameLayout caixa = new FrameLayout(c);
        int p = (int) (20 * c.getResources().getDisplayMetrics().density);
        caixa.setPadding(p, p / 2, p, 0);
        caixa.addView(campo);

        new MaterialAlertDialogBuilder(c)
                .setTitle("Endereço do servidor")
                .setMessage("Emulador: http://10.0.2.2:8080\nCelular: http://IP-do-computador:8080")
                .setView(caixa)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Padrão", (d, w) -> Sessao.setServidor(c, Sessao.SERVIDOR_PADRAO))
                .setPositiveButton("Salvar", (d, w) -> Sessao.setServidor(c, campo.getText().toString()))
                .show();
    }
}
