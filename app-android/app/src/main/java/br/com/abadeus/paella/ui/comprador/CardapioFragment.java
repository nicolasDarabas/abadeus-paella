package br.com.abadeus.paella.ui.comprador;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.BancoLocal;
import br.com.abadeus.paella.data.Carrinho;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.util.Formato;
import br.com.abadeus.paella.util.StatusUi;

/** Cardapio do evento com vendas abertas. */
public class CardapioFragment extends Fragment implements CardapioAdapter.AoMudar {

    private TextView textoEvento;
    private TextView statusEvento;
    private TextView textoData;
    private TextView textoLocal;
    private TextView textoRegras;
    private Modelos.Evento eventoAtual;
    private TextView avisoOffline;
    private SwipeRefreshLayout atualizar;
    private View barraCarrinho;
    private TextView carrinhoQtd;
    private TextView carrinhoTotal;
    private CardapioAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cardapio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.textoOla)).setText("Olá, " + Sessao.getPrimeiroNome(requireContext()) + "!");
        textoEvento = v.findViewById(R.id.textoEvento);
        statusEvento = v.findViewById(R.id.statusEvento);
        textoData = v.findViewById(R.id.textoData);
        textoLocal = v.findViewById(R.id.textoLocal);
        textoRegras = v.findViewById(R.id.textoRegras);
        textoRegras.setOnClickListener(b -> mostrarRegras());
        avisoOffline = v.findViewById(R.id.avisoOffline);
        atualizar = v.findViewById(R.id.atualizar);
        barraCarrinho = v.findViewById(R.id.barraCarrinho);
        carrinhoQtd = v.findViewById(R.id.carrinhoQtd);
        carrinhoTotal = v.findViewById(R.id.carrinhoTotal);

        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CardapioAdapter(this);
        lista.setAdapter(adapter);

        atualizar.setColorSchemeResources(R.color.terracota, R.color.acafrao, R.color.oliva);
        atualizar.setOnRefreshListener(this::carregar);
        v.findViewById(R.id.botaoCarrinho).setOnClickListener(
                b -> startActivity(new Intent(requireContext(), CheckoutActivity.class)));

        carregar();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            carrinhoMudou();
        }
    }

    /** 1) busca os eventos  2) escolhe o que esta com vendas abertas  3) carrega o cardapio. */
    private void carregar() {
        atualizar.setRefreshing(true);
        ApiClient.get(requireContext()).eventos().enqueue(new Resposta<List<Modelos.Evento>>(requireContext()) {
            @Override
            protected void sucesso(List<Modelos.Evento> eventos) {
                if (!isAdded()) {
                    return;
                }
                Modelos.Evento escolhido = null;
                for (Modelos.Evento e : eventos) {
                    if ("VENDAS_ABERTAS".equals(e.status)) {
                        escolhido = e;
                        break;
                    }
                }
                if (escolhido == null && !eventos.isEmpty()) {
                    escolhido = eventos.get(0);
                }
                if (escolhido == null) {
                    textoEvento.setText("Nenhum evento disponível no momento");
                    atualizar.setRefreshing(false);
                    return;
                }
                carregarCardapio(escolhido.id);
            }

            @Override
            protected void falha(int status, String mensagem) {
                super.falha(status, mensagem);
                atualizar.setRefreshing(false);
            }

            @Override
            protected void semConexao() {
                atualizar.setRefreshing(false);
                usarCache();
            }
        });
    }

    private void carregarCardapio(long eventoId) {
        ApiClient.get(requireContext()).cardapio(eventoId).enqueue(new Resposta<Modelos.Cardapio>(requireContext()) {
            @Override
            protected void sucesso(Modelos.Cardapio c) {
                if (!isAdded()) {
                    return;
                }
                avisoOffline.setVisibility(View.GONE);
                BancoLocal.get(requireContext()).salvarCardapio(c);
                mostrar(c);
            }

            @Override
            protected void semConexao() {
                usarCache();
            }

            @Override
            protected void fim() {
                if (isAdded()) {
                    atualizar.setRefreshing(false);
                }
            }
        });
    }

    private void usarCache() {
        if (!isAdded()) {
            return;
        }
        Modelos.Cardapio c = BancoLocal.get(requireContext()).carregarCardapio();
        avisoOffline.setVisibility(View.VISIBLE);
        if (c != null) {
            mostrar(c);
        } else {
            textoEvento.setText("Sem conexão com o servidor");
        }
    }

    private void mostrar(Modelos.Cardapio c) {
        Carrinho.get().setEvento(c.evento.id);
        textoEvento.setText(c.evento.titulo);
        textoData.setText(Formato.dataLonga(c.evento.dataEvento) + " · " + Formato.hora(c.evento.dataEvento));
        textoLocal.setText(c.evento.local);
        eventoAtual = c.evento;
        boolean temInfo = c.evento.regras != null || c.evento.contato != null;
        textoRegras.setVisibility(temInfo ? View.VISIBLE : View.GONE);
        statusEvento.setVisibility(View.VISIBLE);
        statusEvento.setText(StatusUi.evento(c.evento.status));
        StatusUi.pintar(statusEvento, c.evento.status);
        adapter.setItens(c.itens, c.vendasAbertas);
        carrinhoMudou();
    }

    /** Informacoes do evento: horario de retirada, regras e canais de contato. */
    private void mostrarRegras() {
        if (eventoAtual == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        String janela = Formato.janela(eventoAtual.retiradaInicio, eventoAtual.retiradaFim);
        if (!janela.isEmpty()) {
            sb.append("Retirada: ").append(janela).append("\n").append(eventoAtual.local).append("\n\n");
        }
        if (eventoAtual.regras != null) {
            sb.append(eventoAtual.regras).append("\n\n");
        }
        if (eventoAtual.contato != null) {
            sb.append("Dúvidas: ").append(eventoAtual.contato);
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Informações do evento")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Entendi", null)
                .show();
    }

    @Override
    public void carrinhoMudou() {
        Carrinho carrinho = Carrinho.get();
        int qtd = carrinho.totalItens();
        barraCarrinho.setVisibility(qtd > 0 ? View.VISIBLE : View.GONE);
        carrinhoQtd.setText(qtd + (qtd == 1 ? " item no pedido" : " itens no pedido"));
        carrinhoTotal.setText(Formato.dinheiro(carrinho.total()));
    }
}
