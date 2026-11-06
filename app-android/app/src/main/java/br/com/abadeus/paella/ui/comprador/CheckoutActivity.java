package br.com.abadeus.paella.ui.comprador;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Carrinho;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.ui.pedido.PedidoDetalheActivity;
import br.com.abadeus.paella.util.Formato;

/**
 * Revisao do carrinho e escolha do pagamento.
 * Cria o pedido (reserva o estoque) e ja inicia o pagamento escolhido.
 */
public class CheckoutActivity extends BaseActivity {

    private MaterialButton botao;
    private RadioGroup grupo;
    private TextInputEditText observacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        Carrinho carrinho = Carrinho.get();
        if (carrinho.vazio()) {
            finish();
            return;
        }

        LinearLayout lista = findViewById(R.id.listaItens);
        LayoutInflater inf = LayoutInflater.from(this);
        for (Modelos.CardapioItem p : carrinho.itens()) {
            int qtd = carrinho.quantidade(p.produtoId);
            View linha = inf.inflate(R.layout.linha_item, lista, false);
            ((TextView) linha.findViewById(R.id.linhaQtd)).setText(qtd + "x");
            ((TextView) linha.findViewById(R.id.linhaNome)).setText(p.nome);
            ((TextView) linha.findViewById(R.id.linhaValor)).setText(Formato.dinheiro(p.precoAtual * qtd));
            lista.addView(linha);
        }
        ((TextView) findViewById(R.id.textoTotal)).setText(Formato.dinheiro(carrinho.total()));

        grupo = findViewById(R.id.grupoPagamento);
        observacao = findViewById(R.id.editObservacao);
        botao = findViewById(R.id.botaoConfirmar);
        botao.setText("Confirmar · " + Formato.dinheiro(carrinho.total()));
        botao.setOnClickListener(v -> confirmar());
    }

    private String metodoEscolhido() {
        int id = grupo.getCheckedRadioButtonId();
        if (id == R.id.opcaoCartao) {
            return "CARTAO";
        }
        if (id == R.id.opcaoDinheiro) {
            return "DINHEIRO";
        }
        return "PIX";
    }

    private void confirmar() {
        botao.setEnabled(false);
        String obs = observacao.getText() == null ? null : observacao.getText().toString().trim();
        Modelos.CriarPedidoRequest req = Carrinho.get().paraPedido(obs == null || obs.isEmpty() ? null : obs);

        // 1) cria o pedido
        ApiClient.get(this).criarPedido(req).enqueue(new Resposta<Modelos.Pedido>(this) {
            @Override
            protected void sucesso(Modelos.Pedido pedido) {
                Carrinho.get().limpar();
                iniciarPagamento(pedido);
            }

            @Override
            protected void falha(int status, String mensagem) {
                super.falha(status, mensagem);
                botao.setEnabled(true);
            }

            @Override
            protected void semConexao() {
                super.semConexao();
                botao.setEnabled(true);
            }
        });
    }

    // 2) inicia o pagamento e abre o detalhe do pedido
    private void iniciarPagamento(Modelos.Pedido pedido) {
        ApiClient.get(this).iniciarPagamento(pedido.id, new Modelos.IniciarPagamentoRequest(metodoEscolhido()))
                .enqueue(new Resposta<Modelos.Pagamento>(this) {
                    @Override
                    protected void sucesso(Modelos.Pagamento pagamento) {
                        abrirPedido(pedido.id);
                    }

                    @Override
                    protected void falha(int status, String mensagem) {
                        super.falha(status, mensagem);
                        abrirPedido(pedido.id);
                    }

                    @Override
                    protected void semConexao() {
                        abrirPedido(pedido.id);
                    }
                });
    }

    private void abrirPedido(long pedidoId) {
        Intent i = new Intent(this, PedidoDetalheActivity.class);
        i.putExtra(PedidoDetalheActivity.EXTRA_ID, pedidoId);
        startActivity(i);
        finish();
    }
}
