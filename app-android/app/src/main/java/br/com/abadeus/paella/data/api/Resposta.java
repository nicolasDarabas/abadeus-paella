package br.com.abadeus.paella.data.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.ui.auth.LoginActivity;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback padrao do app: trata erro da API, falta de conexao e sessao expirada.
 * Uso: api.algo().enqueue(new Resposta<Tipo>(this) { protected void sucesso(Tipo t) {...} });
 */
public abstract class Resposta<T> implements Callback<T> {

    protected final Context context;

    /** Motivo tecnico da ultima falha de conexao (aparece na mensagem e no Logcat). */
    protected String detalheErro = "";

    protected Resposta(Context context) {
        this.context = context;
    }

    /** Chamado com o corpo da resposta (HTTP 2xx). */
    protected abstract void sucesso(T corpo);

    /** Erro devolvido pela API (mensagem ja traduzida). Padrao: Toast. */
    protected void falha(int status, String mensagem) {
        aviso(mensagem);
    }

    /** Servidor fora do ar ou sem internet. Padrao: Toast. */
    protected void semConexao() {
        aviso("Sem conexão com o servidor (" + Sessao.getServidor(context) + ").\n" + detalheErro);
    }

    /** Sempre chamado no final (sucesso ou erro). Bom para esconder o "carregando". */
    protected void fim() {
    }

    @Override
    public final void onResponse(Call<T> call, Response<T> response) {
        if (!ativo()) {
            return;
        }
        try {
            if (response.isSuccessful()) {
                sucesso(response.body());
                return;
            }
            int codigo = response.code();
            if (codigo == 401 && Sessao.getToken(context) != null) {
                sessaoExpirada();
                return;
            }
            falha(codigo, lerMensagem(response.errorBody(), codigo));
        } finally {
            fim();
        }
    }

    @Override
    public final void onFailure(Call<T> call, Throwable t) {
        if (!ativo()) {
            return;
        }
        detalheErro = t.getClass().getSimpleName() + ": " + t.getMessage();
        Log.e("ABADEUS", "Falha na chamada " + call.request().url(), t);
        try {
            semConexao();
        } finally {
            fim();
        }
    }

    protected void aviso(String mensagem) {
        Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show();
    }

    private boolean ativo() {
        if (context instanceof Activity) {
            Activity a = (Activity) context;
            return !a.isFinishing() && !a.isDestroyed();
        }
        return true;
    }

    private void sessaoExpirada() {
        Sessao.sair(context);
        Toast.makeText(context, "Sua sessão expirou. Entre novamente.", Toast.LENGTH_LONG).show();
        Intent i = new Intent(context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(i);
    }

    private static String lerMensagem(ResponseBody corpo, int codigo) {
        try {
            if (corpo != null) {
                Modelos.Erro erro = new Gson().fromJson(corpo.string(), Modelos.Erro.class);
                if (erro != null) {
                    if (erro.campos != null && !erro.campos.isEmpty()) {
                        return erro.campos.values().iterator().next();
                    }
                    if (erro.mensagem != null) {
                        return erro.mensagem;
                    }
                }
            }
        } catch (Exception ignored) {
            // corpo nao era JSON
        }
        return "Erro " + codigo + " no servidor.";
    }
}
