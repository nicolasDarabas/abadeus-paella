package br.com.abadeus.paella.ui.auth;

import android.os.Bundle;
import android.util.Patterns;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.MainActivity;
import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;

/** Cadastro de novo COMPRADOR (botao REGISTRAR do login). */
public class RegistroActivity extends BaseActivity {

    private TextInputLayout campoNome;
    private TextInputLayout campoEmail;
    private TextInputLayout campoSenha;
    private TextInputEditText editNome;
    private TextInputEditText editEmail;
    private TextInputEditText editTelefone;
    private TextInputEditText editSenha;
    private MaterialButton botao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoNome = findViewById(R.id.campoNome);
        campoEmail = findViewById(R.id.campoEmail);
        campoSenha = findViewById(R.id.campoSenhaRegistro);
        editNome = findViewById(R.id.editNome);
        editEmail = findViewById(R.id.editEmail);
        editTelefone = findViewById(R.id.editTelefone);
        editSenha = findViewById(R.id.editSenhaRegistro);
        botao = findViewById(R.id.botaoCriarConta);
        botao.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        String nome = LoginActivity.texto(editNome);
        String email = LoginActivity.texto(editEmail);
        String telefone = LoginActivity.texto(editTelefone);
        String senha = LoginActivity.texto(editSenha);

        boolean ok = true;
        campoNome.setError(null);
        campoEmail.setError(null);
        campoSenha.setError(null);
        if (nome.length() < 3) {
            campoNome.setError("Informe seu nome");
            ok = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError("E-mail inválido");
            ok = false;
        }
        if (senha.length() < 6) {
            campoSenha.setError("A senha deve ter pelo menos 6 caracteres");
            ok = false;
        }
        if (!ok) {
            return;
        }

        botao.setEnabled(false);
        Modelos.RegistroRequest req = new Modelos.RegistroRequest(nome, email,
                telefone.isEmpty() ? null : telefone, senha);
        ApiClient.get(this).registrar(req).enqueue(new Resposta<Modelos.TokenResponse>(this) {
            @Override
            protected void sucesso(Modelos.TokenResponse t) {
                Sessao.salvarLogin(RegistroActivity.this, t);
                aviso("Conta criada! Bem-vindo(a), " + Sessao.getPrimeiroNome(RegistroActivity.this) + ".");
                startActivity(MainActivity.telaInicial(RegistroActivity.this));
                finish();
            }

            @Override
            protected void falha(int status, String mensagem) {
                if (status == 409) {
                    campoEmail.setError(mensagem);
                } else {
                    aviso(mensagem);
                }
            }

            @Override
            protected void fim() {
                botao.setEnabled(true);
            }
        });
    }
}
