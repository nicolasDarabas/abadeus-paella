package br.com.abadeus.paella.data.api;

import java.util.List;
import java.util.Map;

import br.com.abadeus.paella.data.api.Modelos.Cardapio;
import br.com.abadeus.paella.data.api.Modelos.CancelarRequest;
import br.com.abadeus.paella.data.api.Modelos.Comprovante;
import br.com.abadeus.paella.data.api.Modelos.ConsultaRetirada;
import br.com.abadeus.paella.data.api.Modelos.CriarPedidoRequest;
import br.com.abadeus.paella.data.api.Modelos.Evento;
import br.com.abadeus.paella.data.api.Modelos.IniciarPagamentoRequest;
import br.com.abadeus.paella.data.api.Modelos.LoginRequest;
import br.com.abadeus.paella.data.api.Modelos.Pagamento;
import br.com.abadeus.paella.data.api.Modelos.Pedido;
import br.com.abadeus.paella.data.api.Modelos.PendenteRetirada;
import br.com.abadeus.paella.data.api.Modelos.Produto;
import br.com.abadeus.paella.data.api.Modelos.ProdutoRequest;
import br.com.abadeus.paella.data.api.Modelos.RegistroRequest;
import br.com.abadeus.paella.data.api.Modelos.Resumo;
import br.com.abadeus.paella.data.api.Modelos.Retirada;
import br.com.abadeus.paella.data.api.Modelos.StatusEventoRequest;
import br.com.abadeus.paella.data.api.Modelos.SyncResultado;
import br.com.abadeus.paella.data.api.Modelos.SyncRetiradasRequest;
import br.com.abadeus.paella.data.api.Modelos.TokenResponse;
import br.com.abadeus.paella.data.api.Modelos.Usuario;
import br.com.abadeus.paella.data.api.Modelos.ValidarRetiradaRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Streaming;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Rotas da API Paella ABADEUS (backend Spring Boot). */
public interface ApiService {

    @GET("api/status")
    Call<Map<String, Object>> status();

    // ---------- Login ----------
    @POST("api/auth/login")
    Call<TokenResponse> login(@Body LoginRequest req);

    @POST("api/auth/registrar")
    Call<TokenResponse> registrar(@Body RegistroRequest req);

    @GET("api/auth/me")
    Call<Usuario> me();

    // ---------- Eventos ----------
    @GET("api/eventos")
    Call<List<Evento>> eventos();

    @GET("api/eventos/{id}/cardapio")
    Call<Cardapio> cardapio(@Path("id") long eventoId);

    @PUT("api/eventos/{id}")
    Call<Evento> atualizarEvento(@Path("id") long eventoId, @Body Modelos.EventoRequest req);

    @GET("api/eventos/{id}/produtos")
    Call<List<Produto>> produtosDoEvento(@Path("id") long eventoId);

    @PUT("api/produtos/{id}")
    Call<Produto> atualizarProduto(@Path("id") long produtoId, @Body ProdutoRequest req);

    @GET("api/eventos/{id}/lotes")
    Call<List<Modelos.Lote>> lotes(@Path("id") long eventoId);

    @POST("api/eventos/{id}/lotes")
    Call<Modelos.Lote> criarLote(@Path("id") long eventoId, @Body Modelos.LoteRequest req);

    @POST("api/lotes/{id}/produtos")
    Call<Modelos.Lote> definirProdutoNoLote(@Path("id") long loteId, @Body Modelos.LoteProdutoRequest req);

    @PATCH("api/eventos/{id}/status")
    Call<Evento> alterarStatusEvento(@Path("id") long eventoId, @Body StatusEventoRequest req);

    @POST("api/eventos/{id}/produtos")
    Call<Produto> criarProduto(@Path("id") long eventoId, @Body ProdutoRequest req);

    // ---------- Pedidos ----------
    @POST("api/pedidos")
    Call<Pedido> criarPedido(@Body CriarPedidoRequest req);

    @GET("api/pedidos/meus")
    Call<List<Pedido>> meusPedidos();

    @GET("api/pedidos/{id}")
    Call<Pedido> pedido(@Path("id") long pedidoId);

    @POST("api/pedidos/{id}/cancelar")
    Call<Pedido> cancelar(@Path("id") long pedidoId, @Body CancelarRequest req);

    @GET("api/pedidos/{id}/historico")
    Call<List<Modelos.Historico>> historico(@Path("id") long pedidoId);

    @GET("api/notificacoes/minhas")
    Call<List<Modelos.Notificacao>> meusAvisos();

    @GET("api/pedidos/{id}/comprovante")
    Call<Comprovante> comprovante(@Path("id") long pedidoId);

    // ---------- Pagamento ----------
    @POST("api/pedidos/{id}/pagamentos")
    Call<Pagamento> iniciarPagamento(@Path("id") long pedidoId, @Body IniciarPagamentoRequest req);

    @POST("api/pagamentos/{id}/simular-aprovacao")
    Call<Pedido> simularAprovacao(@Path("id") long pagamentoId);

    @POST("api/pagamentos/{id}/confirmar")
    Call<Pedido> confirmarPagamento(@Path("id") long pagamentoId);

    // ---------- Administrador ----------
    @GET("api/admin/eventos/{id}/resumo")
    Call<Resumo> resumo(@Path("id") long eventoId);

    @GET("api/admin/pedidos")
    Call<List<Pedido>> pedidosAdmin(@Query("eventoId") Long eventoId, @Query("status") String status);

    @POST("api/admin/pedidos/{id}/reembolsar")
    Call<Pedido> reembolsar(@Path("id") long pedidoId, @Body CancelarRequest req);

    @POST("api/admin/usuarios")
    Call<Usuario> criarUsuario(@Body Modelos.NovoUsuarioRequest req);

    /** Relatorio CSV: tipo = "pedidos" ou "arrecadacao". */
    @Streaming
    @Headers("Accept: text/csv, */*")
    @GET("api/admin/relatorios/{tipo}.csv")
    Call<ResponseBody> relatorioCsv(@Path("tipo") String tipo, @Query("eventoId") long eventoId);

    @GET("api/admin/retiradas/consultar")
    Call<ConsultaRetirada> consultarRetirada(@Query("codigo") String codigo);

    @POST("api/admin/retiradas/validar")
    Call<Retirada> validarRetirada(@Body ValidarRetiradaRequest req);

    // ---------- Modo contingencia ----------
    @GET("api/sync/eventos/{id}/retiradas-pendentes")
    Call<List<PendenteRetirada>> retiradasPendentes(@Path("id") long eventoId);

    @POST("api/sync/retiradas")
    Call<List<SyncResultado>> sincronizarRetiradas(@Body SyncRetiradasRequest req);
}
