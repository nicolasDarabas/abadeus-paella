package br.com.abadeus.paella.ui.pedido;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.api.Modelos.Pedido;
import br.com.abadeus.paella.util.Formato;
import br.com.abadeus.paella.util.StatusUi;

/** Lista de pedidos (comprador: "Meus pedidos"; administrador: todos os pedidos). */
public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.Item> {

    private final List<Pedido> pedidos = new ArrayList<>();
    private final boolean mostrarCliente;

    public PedidoAdapter(boolean mostrarCliente) {
        this.mostrarCliente = mostrarCliente;
    }

    public void setPedidos(List<Pedido> novos) {
        pedidos.clear();
        if (novos != null) {
            pedidos.addAll(novos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Item onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Item(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Item h, int position) {
        Pedido p = pedidos.get(position);
        h.numero.setText(p.numero);
        h.status.setText(StatusUi.pedido(p.status));
        StatusUi.pintar(h.status, p.status);
        h.cliente.setVisibility(mostrarCliente ? View.VISIBLE : View.GONE);
        h.cliente.setText(p.usuarioNome + " · " + p.origem);
        h.itens.setText(p.resumoItens());
        h.data.setText(Formato.dataHora(p.criadoEm));
        h.total.setText(Formato.dinheiro(p.valorTotal));
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), PedidoDetalheActivity.class);
            i.putExtra(PedidoDetalheActivity.EXTRA_ID, (long) p.id);
            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return pedidos.size();
    }

    static class Item extends RecyclerView.ViewHolder {
        final TextView numero;
        final TextView status;
        final TextView cliente;
        final TextView itens;
        final TextView data;
        final TextView total;

        Item(View v) {
            super(v);
            numero = v.findViewById(R.id.pedidoNumero);
            status = v.findViewById(R.id.pedidoStatus);
            cliente = v.findViewById(R.id.pedidoCliente);
            itens = v.findViewById(R.id.pedidoItens);
            data = v.findViewById(R.id.pedidoData);
            total = v.findViewById(R.id.pedidoTotal);
        }
    }
}
