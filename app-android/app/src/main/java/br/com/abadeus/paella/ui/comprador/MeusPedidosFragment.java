package br.com.abadeus.paella.ui.comprador;

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
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.ui.pedido.PedidoAdapter;

/** "Meus pedidos" do comprador. */
public class MeusPedidosFragment extends Fragment {

    private SwipeRefreshLayout atualizar;
    private TextView vazia;
    private PedidoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_pedidos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.tituloLista)).setText("Meus pedidos");
        ((TextView) v.findViewById(R.id.subtituloLista)).setText("Toque em um pedido pago para ver o QR Code.");
        vazia = v.findViewById(R.id.listaVazia);
        atualizar = v.findViewById(R.id.atualizar);
        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PedidoAdapter(false);
        lista.setAdapter(adapter);
        atualizar.setColorSchemeResources(R.color.terracota, R.color.acafrao, R.color.oliva);
        atualizar.setOnRefreshListener(this::carregar);
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        ApiClient.get(requireContext()).meusPedidos().enqueue(new Resposta<List<Modelos.Pedido>>(requireContext()) {
            @Override
            protected void sucesso(List<Modelos.Pedido> pedidos) {
                if (!isAdded()) {
                    return;
                }
                adapter.setPedidos(pedidos);
                vazia.setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);
                vazia.setText("Você ainda não fez nenhum pedido.\nVá ao Cardápio para escolher sua paella!");
            }

            @Override
            protected void semConexao() {
                if (!isAdded()) {
                    return;
                }
                List<Modelos.Pedido> salvos = BancoLocal.get(requireContext()).pedidosSalvos();
                adapter.setPedidos(salvos);
                vazia.setVisibility(View.VISIBLE);
                vazia.setText(salvos.isEmpty()
                        ? "Sem conexão com o servidor."
                        : "Sem conexão. Mostrando os pedidos pagos salvos no aparelho (QR Code disponível).");
            }

            @Override
            protected void fim() {
                if (isAdded()) {
                    atualizar.setRefreshing(false);
                }
            }
        });
    }
}
