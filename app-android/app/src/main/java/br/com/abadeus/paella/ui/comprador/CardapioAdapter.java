package br.com.abadeus.paella.ui.comprador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Carrinho;
import br.com.abadeus.paella.data.api.Modelos.CardapioItem;
import br.com.abadeus.paella.util.Formato;

/** Lista de produtos do cardapio com botoes + e -. */
public class CardapioAdapter extends RecyclerView.Adapter<CardapioAdapter.Item> {

    public interface AoMudar {
        void carrinhoMudou();
    }

    private final List<CardapioItem> itens = new ArrayList<>();
    private final AoMudar aoMudar;
    private boolean podeComprar = true;

    public CardapioAdapter(AoMudar aoMudar) {
        this.aoMudar = aoMudar;
    }

    public void setItens(List<CardapioItem> novos, boolean podeComprar) {
        itens.clear();
        if (novos != null) {
            itens.addAll(novos);
        }
        this.podeComprar = podeComprar;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Item onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produto, parent, false);
        return new Item(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Item h, int position) {
        CardapioItem p = itens.get(position);
        Carrinho carrinho = Carrinho.get();
        int qtd = carrinho.quantidade(p.produtoId);

        h.nome.setText(p.nome);
        h.descricao.setText(p.descricao);
        h.descricao.setVisibility(p.descricao == null || p.descricao.isEmpty() ? View.GONE : View.VISIBLE);
        h.preco.setText(Formato.dinheiro(p.precoAtual));
        h.lote.setVisibility(p.loteNome == null ? View.GONE : View.VISIBLE);
        h.lote.setText(p.loteNome);

        if (p.esgotado) {
            h.estoque.setText("Esgotado");
            h.estoque.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.terracota));
        } else if (p.disponivel != null && p.disponivel <= 20) {
            h.estoque.setText("Últimas " + p.disponivel + " unidades!");
            h.estoque.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.terracota));
        } else {
            h.estoque.setText(p.disponivel == null ? "Disponível" : "Disponível · " + p.disponivel + " restantes");
            h.estoque.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.oliva));
        }

        h.qtd.setText(String.valueOf(qtd));
        h.mais.setEnabled(podeComprar && !p.esgotado);
        h.menos.setEnabled(podeComprar && qtd > 0);
        h.itemView.setAlpha(p.esgotado ? 0.55f : 1f);

        h.mais.setOnClickListener(v -> {
            if (!carrinho.adicionar(p)) {
                Toast.makeText(v.getContext(), "Quantidade máxima disponível atingida.", Toast.LENGTH_SHORT).show();
            }
            notifyItemChanged(h.getBindingAdapterPosition());
            aoMudar.carrinhoMudou();
        });
        h.menos.setOnClickListener(v -> {
            carrinho.remover(p);
            notifyItemChanged(h.getBindingAdapterPosition());
            aoMudar.carrinhoMudou();
        });
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class Item extends RecyclerView.ViewHolder {
        final TextView nome;
        final TextView descricao;
        final TextView preco;
        final TextView lote;
        final TextView estoque;
        final TextView qtd;
        final MaterialButton mais;
        final MaterialButton menos;

        Item(View v) {
            super(v);
            nome = v.findViewById(R.id.produtoNome);
            descricao = v.findViewById(R.id.produtoDescricao);
            preco = v.findViewById(R.id.produtoPreco);
            lote = v.findViewById(R.id.produtoLote);
            estoque = v.findViewById(R.id.produtoEstoque);
            qtd = v.findViewById(R.id.produtoQtd);
            mais = v.findViewById(R.id.botaoMais);
            menos = v.findViewById(R.id.botaoMenos);
        }
    }
}
