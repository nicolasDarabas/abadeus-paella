package br.com.abadeus.paella.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.abadeus.paella.data.api.Modelos.CardapioItem;
import br.com.abadeus.paella.data.api.Modelos.CriarPedidoRequest;
import br.com.abadeus.paella.data.api.Modelos.ItemRequest;

/** Carrinho de compras do comprador (fica na memoria enquanto o app esta aberto). */
public final class Carrinho {

    private static final Carrinho INSTANCIA = new Carrinho();

    private long eventoId = -1;
    private final Map<Long, CardapioItem> produtos = new LinkedHashMap<>();
    private final Map<Long, Integer> quantidades = new LinkedHashMap<>();

    private Carrinho() {
    }

    public static Carrinho get() {
        return INSTANCIA;
    }

    public void setEvento(long id) {
        if (id != eventoId) {
            limpar();
            eventoId = id;
        }
    }

    public long getEventoId() {
        return eventoId;
    }

    public int quantidade(long produtoId) {
        Integer q = quantidades.get(produtoId);
        return q == null ? 0 : q;
    }

    /** Retorna false se nao puder adicionar (limite de estoque). */
    public boolean adicionar(CardapioItem item) {
        int atual = quantidade(item.produtoId);
        if (item.esgotado || (item.disponivel != null && atual >= item.disponivel) || atual >= 50) {
            return false;
        }
        produtos.put(item.produtoId, item);
        quantidades.put(item.produtoId, atual + 1);
        return true;
    }

    public void remover(CardapioItem item) {
        int atual = quantidade(item.produtoId);
        if (atual <= 1) {
            quantidades.remove(item.produtoId);
            produtos.remove(item.produtoId);
        } else {
            quantidades.put(item.produtoId, atual - 1);
        }
    }

    public int totalItens() {
        int t = 0;
        for (Integer q : quantidades.values()) {
            t += q;
        }
        return t;
    }

    public double total() {
        double t = 0;
        for (Map.Entry<Long, Integer> e : quantidades.entrySet()) {
            CardapioItem p = produtos.get(e.getKey());
            if (p != null) {
                t += p.precoAtual * e.getValue();
            }
        }
        return t;
    }

    public boolean vazio() {
        return quantidades.isEmpty();
    }

    public List<CardapioItem> itens() {
        return new ArrayList<>(produtos.values());
    }

    public void limpar() {
        produtos.clear();
        quantidades.clear();
    }

    public CriarPedidoRequest paraPedido(String observacao) {
        CriarPedidoRequest req = new CriarPedidoRequest();
        req.eventoId = eventoId;
        req.observacao = observacao;
        for (Map.Entry<Long, Integer> e : quantidades.entrySet()) {
            req.itens.add(new ItemRequest(e.getKey(), e.getValue()));
        }
        return req;
    }
}
