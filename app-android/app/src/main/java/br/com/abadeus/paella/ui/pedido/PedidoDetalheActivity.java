package br.com.abadeus.paella.ui.pedido;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.abadeus.paella.BaseActivity;
import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.BancoLocal;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.util.Formato;
import br.com.abadeus.paella.util.StatusUi;

/**
 * Detalhe do pedido.
 * - Aguardando pagamento: Pix "copia e cola", simular aprovacao (comprador) ou confirmar (administrador).
 * - Pago: QR Code para a retirada (fica salvo no aparelho para funcionar sem internet).
 */
public class PedidoDetalheActivity extends BaseActivity {

    public static final String EXTRA_ID = "pedidoId";

    private long pedidoId;
    private boolean admin;
    private Modelos.Pedido pedido;

    private SwipeRefreshLayout atualizar;
    private TextView avisoOffline;
    private TextView numero;
    private TextView status;
    private TextView evento;
    private TextView info;
    private View cardQr;
    private ImageView qrImagem;
    private TextView qrRetirado;
    private TextView qrCodigo;
    private TextView qrRetirada;
    private View cardPagamento;
    private TextView pagMetodo;
    private TextView pagPrazo;
    private View blocoPix;
    private TextView pixCodigo;
    private TextView textoDinheiro;
    private MaterialButton botaoSimular;
    private MaterialButton botaoConfirmar;
    private MaterialButton botaoNovoPix;
    private MaterialButton botaoCancelar;
    private MaterialButton botaoReembolsar;
    private View cardHistorico;
    private LinearLayout listaHistorico;
    private LinearLayout listaItens;
    private TextView total;
    private TextView observacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedido_detalhe);
        pedidoId = getIntent().getLongExtra(EXTRA_ID, -1);
        admin = Sessao.isAdministrador(this);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        atualizar = findViewById(R.id.atualizar);
        avisoOffline = findViewById(R.id.avisoOffline);
        numero = findViewById(R.id.detNumero);
        status = findViewById(R.id.detStatus);
        evento = findViewById(R.id.detEvento);
        info = findViewById(R.id.detInfo);
        cardQr = findViewById(R.id.cardQr);
        qrImagem = findViewById(R.id.qrImagem);
        qrRetirado = findViewById(R.id.qrRetirado);
        qrCodigo = findViewById(R.id.qrCodigo);
        qrRetirada = findViewById(R.id.qrRetirada);
        cardPagamento = findViewById(R.id.cardPagamento);
        pagMetodo = findViewById(R.id.pagMetodo);
        pagPrazo = findViewById(R.id.pagPrazo);
        blocoPix = findViewById(R.id.blocoPix);
        pixCodigo = findViewById(R.id.pixCodigo);
        textoDinheiro = findViewById(R.id.textoDinheiro);
        botaoSimular = findViewById(R.id.botaoSimular);
        botaoConfirmar = findViewById(R.id.botaoConfirmarPagamento);
        botaoNovoPix = findViewById(R.id.botaoNovoPix);
        botaoCancelar = findViewById(R.id.botaoCancelar);
        botaoReembolsar = findViewById(R.id.botaoReembolsar);
        cardHistorico = findViewById(R.id.cardHistorico);
        listaHistorico = findViewById(R.id.listaHistorico);
        listaItens = findViewById(R.id.listaItens);
        total = findViewById(R.id.detTotal);
        observacao = findViewById(R.id.detObservacao);

        atualizar.setColorSchemeResources(R.color.terracota, R.color.acafrao, R.color.oliva);
        atualizar.setOnRefreshListener(this::carregar);
        findViewById(R.id.botaoCopiar).setOnClickListener(v -> copiarPix());
        botaoSimular.setOnClickListener(v -> aprovarPagamento(false));
        botaoConfirmar.setOnClickListener(v -> aprovarPagamento(true));
        botaoNovoPix.setOnClickListener(v -> novoPix());
        botaoCancelar.setOnClickListener(v -> confirmarCancelamento());
        botaoReembolsar.setOnClickListener(v -> confirmarReembolso());

        carregar();
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        ApiClient.get(this).pedido(pedidoId).enqueue(new Resposta<Modelos.Pedido>(this) {
            @Override
            protected void sucesso(Modelos.Pedido p) {
                avisoOffline.setVisibility(View.GONE);
                mostrar(p);
                carregarHistorico();
            }

            @Override
            protected void semConexao() {
                mostrarOffline();
            }

            @Override
            protected void fim() {
                atualizar.setRefreshing(false);
            }
        });
    }

    private void mostrar(Modelos.Pedido p) {
        pedido = p;
        numero.setText(p.numero);
        status.setText(StatusUi.pedido(p.status));
        StatusUi.pintar(status, p.status);
        evento.setText(p.eventoTitulo);
        String quem = admin ? p.usuarioNome + " · " + p.origem + " · " : "";
        info.setText(quem + "Feito em " + Formato.dataHora(p.criadoEm));

        // Itens
        listaItens.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (Modelos.PedidoItem i : p.itens) {
            View linha = inf.inflate(R.layout.linha_item, listaItens, false);
            ((TextView) linha.findViewById(R.id.linhaQtd)).setText(i.quantidade + "x");
            String nome = i.loteNome == null ? i.produtoNome : i.produtoNome + "\n" + i.loteNome;
            ((TextView) linha.findViewById(R.id.linhaNome)).setText(nome);
            ((TextView) linha.findViewById(R.id.linhaValor)).setText(Formato.dinheiro(i.subtotal));
            listaItens.addView(linha);
        }
        total.setText(Formato.dinheiro(p.valorTotal));
        observacao.setVisibility(p.observacao == null ? View.GONE : View.VISIBLE);
        observacao.setText("Observação: " + p.observacao);

        // Pagamento
        boolean aguardando = "AGUARDANDO_PAGAMENTO".equals(p.status);
        cardPagamento.setVisibility(aguardando ? View.VISIBLE : View.GONE);
        blocoPix.setVisibility(View.GONE);
        textoDinheiro.setVisibility(View.GONE);
        botaoSimular.setVisibility(View.GONE);
        botaoConfirmar.setVisibility(View.GONE);
        botaoNovoPix.setVisibility(View.GONE);
        if (aguardando) {
            pagPrazo.setText(p.expiraEm == null ? ""
                    : "Pague até as " + Formato.hora(p.expiraEm) + " para manter a reserva.");
            Modelos.Pagamento pg = p.pagamentoPendente();
            if (pg == null) {
                pagMetodo.setText("Nenhum pagamento em andamento.");
                botaoNovoPix.setVisibility(View.VISIBLE);
            } else {
                pagMetodo.setText(StatusUi.metodo(pg.metodo) + " · " + Formato.dinheiro(pg.valor));
                if ("PIX".equals(pg.metodo) && pg.codigoPix != null) {
                    blocoPix.setVisibility(View.VISIBLE);
                    pixCodigo.setText(pg.codigoPix);
                }
                if (admin) {
                    botaoConfirmar.setVisibility(View.VISIBLE);
                } else if ("DINHEIRO".equals(pg.metodo)) {
                    textoDinheiro.setVisibility(View.VISIBLE);
                    textoDinheiro.setText("Vá ao caixa do evento e informe o número " + p.numero
                            + ". Assim que a organização confirmar, seu QR Code aparece aqui.");
                } else {
                    botaoSimular.setVisibility(View.VISIBLE);
                    botaoSimular.setText("PIX".equals(pg.metodo)
                            ? "Simular Pix pago" : "Simular cartão aprovado");
                }
            }
        }

        botaoCancelar.setVisibility(aguardando ? View.VISIBLE : View.GONE);
        botaoReembolsar.setVisibility(admin && "PAGO".equals(p.status) ? View.VISIBLE : View.GONE);

        // QR Code
        if (p.qrCodeDisponivel) {
            carregarComprovante();
        } else {
            cardQr.setVisibility(View.GONE);
        }
    }

    private void carregarComprovante() {
        ApiClient.get(this).comprovante(pedidoId).enqueue(new Resposta<Modelos.Comprovante>(this) {
            @Override
            protected void sucesso(Modelos.Comprovante c) {
                BancoLocal.get(PedidoDetalheActivity.this).salvarComprovante(c);
                mostrarQr(c, "RETIRADO".equals(c.status));
            }

            @Override
            protected void semConexao() {
                mostrarOffline();
            }
        });
    }

    private void mostrarQr(Modelos.Comprovante c, boolean retirado) {
        cardQr.setVisibility(View.VISIBLE);
        try {
            byte[] bytes = Base64.decode(c.qrCodeBase64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            qrImagem.setImageBitmap(bmp);
        } catch (Exception e) {
            qrImagem.setImageResource(R.drawable.ic_qr);
        }
        qrCodigo.setText(c.codigoRetirada);
        String janela = Formato.janela(c.retiradaInicio, c.retiradaFim);
        String local = c.local == null ? "" : c.local;
        qrRetirada.setText(janela.isEmpty() ? local : "Retirada: " + janela + "\n" + local);
        qrImagem.setAlpha(retirado ? 0.2f : 1f);
        qrRetirado.setVisibility(retirado ? View.VISIBLE : View.GONE);
        if (retirado && pedido != null && pedido.retiradoEm != null) {
            qrRetirado.setText("RETIRADO · " + Formato.dataHora(pedido.retiradoEm));
        }
    }

    /** Sem internet: usa o comprovante salvo no banco local. */
    private void mostrarOffline() {
        Modelos.Comprovante c = BancoLocal.get(this).carregarComprovante(pedidoId);
        if (c == null) {
            Toast.makeText(this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            return;
        }
        avisoOffline.setVisibility(View.VISIBLE);
        if (pedido == null) {
            numero.setText(c.numero);
            status.setText("Salvo no aparelho");
            StatusUi.pintar(status, "PAGO");
            evento.setText(c.eventoTitulo);
            total.setText(Formato.dinheiro(c.valorTotal));
        }
        mostrarQr(c, false);
    }

    private void copiarPix() {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("Pix", pixCodigo.getText()));
        Toast.makeText(this, "Código Pix copiado", Toast.LENGTH_SHORT).show();
    }

    private void aprovarPagamento(boolean comoAdmin) {
        Modelos.Pagamento pg = pedido == null ? null : pedido.pagamentoPendente();
        if (pg == null) {
            return;
        }
        botaoSimular.setEnabled(false);
        botaoConfirmar.setEnabled(false);
        retrofit2.Call<Modelos.Pedido> chamada = comoAdmin
                ? ApiClient.get(this).confirmarPagamento(pg.id)
                : ApiClient.get(this).simularAprovacao(pg.id);
        chamada.enqueue(new Resposta<Modelos.Pedido>(this) {
            @Override
            protected void sucesso(Modelos.Pedido p) {
                mostrar(p);
                new MaterialAlertDialogBuilder(PedidoDetalheActivity.this)
                        .setIcon(R.drawable.ic_check)
                        .setTitle("Pagamento aprovado!")
                        .setMessage(comoAdmin
                                ? "O pedido " + p.numero + " está pago e liberado para retirada."
                                : "Obrigado por ajudar a ABADEUS! Seu QR Code de retirada já está disponível.")
                        .setPositiveButton("Ver QR Code", null)
                        .show();
            }

            @Override
            protected void fim() {
                botaoSimular.setEnabled(true);
                botaoConfirmar.setEnabled(true);
            }
        });
    }

    /** Linha do tempo: cada mudanca de status, quem fez e quando. */
    private void carregarHistorico() {
        ApiClient.get(this).historico(pedidoId).enqueue(new Resposta<java.util.List<Modelos.Historico>>(this) {
            @Override
            protected void sucesso(java.util.List<Modelos.Historico> lista) {
                listaHistorico.removeAllViews();
                LayoutInflater inf = LayoutInflater.from(PedidoDetalheActivity.this);
                for (Modelos.Historico h : lista) {
                    View linha = inf.inflate(R.layout.linha_historico, listaHistorico, false);
                    ((TextView) linha.findViewById(R.id.histTitulo)).setText(StatusUi.pedido(h.statusNovo));
                    String detalhe = Formato.dataHora(h.alteradoEm) + " · " + h.alteradoPor
                            + (h.motivo == null ? "" : "\n" + h.motivo);
                    ((TextView) linha.findViewById(R.id.histDetalhe)).setText(detalhe);
                    StatusUi.pintar((TextView) linha.findViewById(R.id.histTitulo), h.statusNovo);
                    ((TextView) linha.findViewById(R.id.histTitulo)).setBackground(null);
                    listaHistorico.addView(linha);
                }
                cardHistorico.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            protected void semConexao() {
                // sem internet: o historico simplesmente nao aparece
            }
        });
    }

    private void confirmarReembolso() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reembolsar o pedido " + (pedido == null ? "" : pedido.numero) + "?")
                .setMessage("O QR Code deixa de valer, o estoque é liberado e o comprador recebe um aviso. "
                        + "(Pagamento simulado: nenhum valor real é estornado.)")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Reembolsar", (d, w) ->
                        ApiClient.get(this).reembolsar(pedidoId, new Modelos.CancelarRequest("Reembolsado pela organização"))
                                .enqueue(new Resposta<Modelos.Pedido>(this) {
                                    @Override
                                    protected void sucesso(Modelos.Pedido p) {
                                        mostrar(p);
                                        carregarHistorico();
                                        aviso("Pedido reembolsado.");
                                    }
                                }))
                .show();
    }

    private void novoPix() {
        botaoNovoPix.setEnabled(false);
        ApiClient.get(this).iniciarPagamento(pedidoId, new Modelos.IniciarPagamentoRequest("PIX"))
                .enqueue(new Resposta<Modelos.Pagamento>(this) {
                    @Override
                    protected void sucesso(Modelos.Pagamento pg) {
                        carregar();
                    }

                    @Override
                    protected void fim() {
                        botaoNovoPix.setEnabled(true);
                    }
                });
    }

    private void confirmarCancelamento() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar o pedido " + (pedido == null ? "" : pedido.numero) + "?")
                .setMessage("Os itens voltam para o estoque.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Cancelar pedido", (d, w) ->
                        ApiClient.get(this).cancelar(pedidoId, new Modelos.CancelarRequest(
                                admin ? "Cancelado pela organização" : "Cancelado pelo comprador"))
                                .enqueue(new Resposta<Modelos.Pedido>(this) {
                                    @Override
                                    protected void sucesso(Modelos.Pedido p) {
                                        mostrar(p);
                                        aviso("Pedido cancelado.");
                                    }
                                }))
                .show();
    }

}
