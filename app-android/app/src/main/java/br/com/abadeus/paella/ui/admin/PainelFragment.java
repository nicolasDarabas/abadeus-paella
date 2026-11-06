package br.com.abadeus.paella.ui.admin;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.ui.comprador.PerfilFragment;
import br.com.abadeus.paella.util.Formato;
import br.com.abadeus.paella.util.StatusUi;

/** Painel do administrador: arrecadacao, pedidos, retiradas e vendas por produto. */
public class PainelFragment extends Fragment {

    private static final String[] STATUS_EVENTO = {"RASCUNHO", "VENDAS_ABERTAS", "VENDAS_ENCERRADAS", "FINALIZADO"};
    private static final String[] STATUS_PEDIDO = {"AGUARDANDO_PAGAMENTO", "PAGO", "RETIRADO", "CANCELADO", "EXPIRADO", "REEMBOLSADO"};

    private SwipeRefreshLayout atualizar;
    private TextView painelEvento;
    private TextView painelStatus;
    private TextView painelData;
    private TextView kpiArrecadado;
    private TextView kpiPedidos;
    private TextView kpiARetirar;
    private TextView kpiRetirados;
    private TextView kpiPendente;
    private LinearLayout listaProdutos;
    private LinearLayout listaStatus;

    private Modelos.Evento evento;
    private List<Modelos.Evento> eventos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_painel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.painelOla)).setText("Olá, " + Sessao.getPrimeiroNome(requireContext()));
        atualizar = v.findViewById(R.id.atualizar);
        painelEvento = v.findViewById(R.id.painelEvento);
        painelStatus = v.findViewById(R.id.painelStatusEvento);
        painelData = v.findViewById(R.id.painelData);
        kpiArrecadado = v.findViewById(R.id.kpiArrecadado);
        kpiPedidos = v.findViewById(R.id.kpiPedidos);
        kpiARetirar = v.findViewById(R.id.kpiARetirar);
        kpiRetirados = v.findViewById(R.id.kpiRetirados);
        kpiPendente = v.findViewById(R.id.kpiPendente);
        listaProdutos = v.findViewById(R.id.listaProdutos);
        listaStatus = v.findViewById(R.id.listaStatus);

        atualizar.setColorSchemeResources(R.color.terracota, R.color.acafrao, R.color.oliva);
        atualizar.setOnRefreshListener(this::carregar);
        v.findViewById(R.id.botaoSairAdmin).setOnClickListener(b -> PerfilFragment.confirmarSaida(this));
        v.findViewById(R.id.botaoTrocarEvento).setOnClickListener(b -> trocarEvento());
        v.findViewById(R.id.botaoStatusEvento).setOnClickListener(b -> alterarStatus());
        v.findViewById(R.id.botaoNovoProduto).setOnClickListener(b -> novoProduto());

        // Gestao (dialogos em GestaoDialogs)
        v.findViewById(R.id.botaoEditarEvento).setOnClickListener(b -> {
            if (evento != null) {
                GestaoDialogs.editarEvento(this, evento, this::carregar);
            }
        });
        v.findViewById(R.id.botaoProdutos).setOnClickListener(b -> {
            if (evento != null) {
                GestaoDialogs.produtos(this, evento.id, this::carregar);
            }
        });
        v.findViewById(R.id.botaoLotes).setOnClickListener(b -> {
            if (evento != null) {
                GestaoDialogs.lotes(this, evento.id, this::carregar);
            }
        });
        v.findViewById(R.id.botaoEquipe).setOnClickListener(b -> GestaoDialogs.novoVoluntario(this));
        v.findViewById(R.id.botaoCsvPedidos).setOnClickListener(b -> {
            if (evento != null) {
                GestaoDialogs.exportarCsv(this, evento.id, "pedidos");
            }
        });
        v.findViewById(R.id.botaoCsvArrecadacao).setOnClickListener(b -> {
            if (evento != null) {
                GestaoDialogs.exportarCsv(this, evento.id, "arrecadacao");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        EventoAtual.obter(requireContext(), (ev, todos) -> {
            if (!isAdded()) {
                return;
            }
            eventos = todos;
            evento = ev;
            if (ev == null) {
                painelEvento.setText("Nenhum evento cadastrado");
                atualizar.setRefreshing(false);
                return;
            }
            painelEvento.setText(ev.titulo);
            painelStatus.setText(StatusUi.evento(ev.status));
            StatusUi.pintar(painelStatus, ev.status);
            painelData.setText(Formato.dataLonga(ev.dataEvento) + " · " + ev.local);
            carregarResumo(ev.id);
        });
    }

    private void carregarResumo(long eventoId) {
        ApiClient.get(requireContext()).resumo(eventoId).enqueue(new Resposta<Modelos.Resumo>(requireContext()) {
            @Override
            protected void sucesso(Modelos.Resumo r) {
                if (isAdded()) {
                    mostrar(r);
                }
            }

            @Override
            protected void fim() {
                if (isAdded()) {
                    atualizar.setRefreshing(false);
                }
            }
        });
    }

    private void mostrar(Modelos.Resumo r) {
        kpiArrecadado.setText(Formato.dinheiro(r.valorArrecadado));
        kpiPedidos.setText(String.valueOf(r.totalPedidos));
        kpiARetirar.setText(String.valueOf(r.aRetirar));
        kpiRetirados.setText(String.valueOf(r.retirados));
        kpiPendente.setText("Aguardando pagamento: " + Formato.dinheiro(r.valorAguardandoPagamento));

        LayoutInflater inf = LayoutInflater.from(requireContext());
        listaProdutos.removeAllViews();
        for (Modelos.ProdutoVendido p : r.produtos) {
            View linha = inf.inflate(R.layout.linha_produto_vendido, listaProdutos, false);
            ((TextView) linha.findViewById(R.id.vendNome)).setText(p.nome);
            String numeros = p.limiteEstoque == null
                    ? p.vendidos + " vendidos"
                    : p.vendidos + " / " + p.limiteEstoque;
            ((TextView) linha.findViewById(R.id.vendNumeros)).setText(numeros);
            LinearProgressIndicator barra = linha.findViewById(R.id.vendBarra);
            if (p.limiteEstoque == null || p.limiteEstoque == 0) {
                barra.setVisibility(View.GONE);
            } else {
                barra.setProgress((int) Math.min(100, p.vendidos * 100 / p.limiteEstoque));
            }
            listaProdutos.addView(linha);
        }

        listaStatus.removeAllViews();
        for (String s : STATUS_PEDIDO) {
            Long qtd = r.pedidosPorStatus == null ? null : r.pedidosPorStatus.get(s);
            View linha = inf.inflate(R.layout.linha_item, listaStatus, false);
            ((TextView) linha.findViewById(R.id.linhaQtd)).setText(String.valueOf(qtd == null ? 0 : qtd));
            ((TextView) linha.findViewById(R.id.linhaNome)).setText(StatusUi.pedido(s));
            ((TextView) linha.findViewById(R.id.linhaValor)).setText("");
            listaStatus.addView(linha);
        }
    }

    private void trocarEvento() {
        if (eventos == null || eventos.isEmpty()) {
            return;
        }
        String[] nomes = new String[eventos.size()];
        int marcado = 0;
        for (int i = 0; i < eventos.size(); i++) {
            Modelos.Evento e = eventos.get(i);
            nomes[i] = e.titulo + " (" + StatusUi.evento(e.status) + ")";
            if (evento != null && e.id.equals(evento.id)) {
                marcado = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Escolha o evento")
                .setSingleChoiceItems(nomes, marcado, (d, i) -> {
                    Sessao.setEventoAdmin(requireContext(), eventos.get(i).id);
                    d.dismiss();
                    carregar();
                })
                .show();
    }

    private void alterarStatus() {
        if (evento == null) {
            return;
        }
        String[] nomes = new String[STATUS_EVENTO.length];
        int marcado = 0;
        for (int i = 0; i < STATUS_EVENTO.length; i++) {
            nomes[i] = StatusUi.evento(STATUS_EVENTO[i]);
            if (STATUS_EVENTO[i].equals(evento.status)) {
                marcado = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Status do evento")
                .setSingleChoiceItems(nomes, marcado, (d, i) -> {
                    d.dismiss();
                    ApiClient.get(requireContext())
                            .alterarStatusEvento(evento.id, new Modelos.StatusEventoRequest(STATUS_EVENTO[i]))
                            .enqueue(new Resposta<Modelos.Evento>(requireContext()) {
                                @Override
                                protected void sucesso(Modelos.Evento e) {
                                    aviso("Evento: " + StatusUi.evento(e.status));
                                    if (isAdded()) {
                                        carregar();
                                    }
                                }
                            });
                })
                .show();
    }

    /** Cadastro rapido de produto no evento atual. */
    private void novoProduto() {
        if (evento == null) {
            return;
        }
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(p, p / 2, p, 0);
        EditText nome = campo("Nome do produto", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText preco = campo("Preço (ex.: 45.00)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText limite = campo("Limite de estoque (vazio = sem limite)", InputType.TYPE_CLASS_NUMBER);
        form.addView(nome);
        form.addView(preco);
        form.addView(limite);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Novo produto")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (d, w) -> {
                    try {
                        String n = nome.getText().toString().trim();
                        double valor = Double.parseDouble(preco.getText().toString().replace(',', '.'));
                        String l = limite.getText().toString().trim();
                        Integer lim = l.isEmpty() ? null : Integer.parseInt(l);
                        if (n.isEmpty()) {
                            throw new IllegalArgumentException();
                        }
                        ApiClient.get(requireContext())
                                .criarProduto(evento.id, new Modelos.ProdutoRequest(n, null, valor, lim))
                                .enqueue(new Resposta<Modelos.Produto>(requireContext()) {
                                    @Override
                                    protected void sucesso(Modelos.Produto prod) {
                                        aviso("Produto cadastrado: " + prod.nome);
                                        if (isAdded()) {
                                            carregar();
                                        }
                                    }
                                });
                    } catch (RuntimeException e) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setMessage("Preencha o nome e um preço válido.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                })
                .show();
    }

    private EditText campo(String dica, int tipo) {
        EditText e = new EditText(requireContext());
        e.setHint(dica);
        e.setInputType(tipo);
        return e;
    }
}
