package br.com.abadeus.paella.ui.admin;

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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.ui.pedido.PedidoAdapter;

/** Todos os pedidos do evento, com filtro por status. */
public class PedidosAdminFragment extends Fragment {

    private static final String[] ROTULOS = {"Todos", "Aguardando", "Pagos", "Retirados", "Reembolsados", "Cancelados", "Expirados"};
    private static final String[] FILTROS = {null, "AGUARDANDO_PAGAMENTO", "PAGO", "RETIRADO", "REEMBOLSADO", "CANCELADO", "EXPIRADO"};

    private SwipeRefreshLayout atualizar;
    private TextView vazia;
    private TextView subtitulo;
    private PedidoAdapter adapter;
    private String filtro = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_pedidos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.tituloLista)).setText("Pedidos");
        subtitulo = v.findViewById(R.id.subtituloLista);
        vazia = v.findViewById(R.id.listaVazia);
        atualizar = v.findViewById(R.id.atualizar);

        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PedidoAdapter(true);
        lista.setAdapter(adapter);

        v.findViewById(R.id.rolagemFiltros).setVisibility(View.VISIBLE);
        ChipGroup grupo = v.findViewById(R.id.filtros);
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (int i = 0; i < ROTULOS.length; i++) {
            Chip chip = (Chip) inf.inflate(R.layout.chip_filtro, grupo, false);
            chip.setId(View.generateViewId());
            chip.setText(ROTULOS[i]);
            chip.setTag(FILTROS[i]);
            grupo.addView(chip);
            if (i == 0) {
                chip.setChecked(true);
            }
        }
        grupo.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) {
                return;
            }
            Chip marcado = g.findViewById(ids.get(0));
            filtro = (String) marcado.getTag();
            carregar();
        });

        atualizar.setColorSchemeResources(R.color.terracota, R.color.acafrao, R.color.oliva);
        atualizar.setOnRefreshListener(this::carregar);
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        long eventoId = Sessao.getEventoAdmin(requireContext());
        atualizar.setRefreshing(true);
        ApiClient.get(requireContext())
                .pedidosAdmin(eventoId > 0 ? eventoId : null, filtro)
                .enqueue(new Resposta<List<Modelos.Pedido>>(requireContext()) {
                    @Override
                    protected void sucesso(List<Modelos.Pedido> pedidos) {
                        if (!isAdded()) {
                            return;
                        }
                        adapter.setPedidos(pedidos);
                        double soma = 0;
                        for (Modelos.Pedido p : pedidos) {
                            soma += p.valorTotal;
                        }
                        subtitulo.setText(pedidos.size() + " pedido(s) · "
                                + br.com.abadeus.paella.util.Formato.dinheiro(soma));
                        vazia.setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);
                        vazia.setText("Nenhum pedido neste filtro.");
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
