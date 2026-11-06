package br.com.abadeus.paella.data.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import br.com.abadeus.paella.data.Sessao;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cria o cliente Retrofit apontando para o servidor configurado
 * e envia o token JWT em todas as requisicoes.
 */
public final class ApiClient {

    private static ApiService servico;
    private static String urlAtual;

    private ApiClient() {
    }

    public static synchronized ApiService get(Context context) {
        final Context app = context.getApplicationContext();
        String url = Sessao.getServidor(app);
        if (servico == null || !url.equals(urlAtual)) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request.Builder req = chain.request().newBuilder();
                        // JSON por padrao; chamadas como o relatorio CSV definem o proprio Accept
                        if (chain.request().header("Accept") == null) {
                            req.header("Accept", "application/json");
                        }
                        String token = Sessao.getToken(app);
                        if (token != null) {
                            req.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(req.build());
                    })
                    .build();

            servico = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
            urlAtual = url;
        }
        return servico;
    }
}
