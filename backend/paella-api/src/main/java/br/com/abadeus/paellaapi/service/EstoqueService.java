package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.LoteProduto;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.repository.LoteProdutoRepository;
import br.com.abadeus.paellaapi.repository.PedidoItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Regras de preco e estoque.
 * Pedidos AGUARDANDO_PAGAMENTO reservam estoque; PAGO e RETIRADO consomem.
 */
@Service
public class EstoqueService {

    public static final Set<StatusPedido> STATUS_QUE_OCUPAM_ESTOQUE =
            EnumSet.of(StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.PAGO, StatusPedido.RETIRADO);

    private final PedidoItemRepository itemRepository;
    private final LoteProdutoRepository loteProdutoRepository;

    public EstoqueService(PedidoItemRepository itemRepository, LoteProdutoRepository loteProdutoRepository) {
        this.itemRepository = itemRepository;
        this.loteProdutoRepository = loteProdutoRepository;
    }

    /** Preco aplicado agora e o lote de origem (lote pode ser nulo = preco padrao do produto). */
    public record PrecoVigente(BigDecimal preco, LoteProduto loteProduto) {
    }

    public long vendidos(Long produtoId) {
        return itemRepository.somarQuantidadeProduto(produtoId, STATUS_QUE_OCUPAM_ESTOQUE);
    }

    public long vendidosNoLote(Long produtoId, Long loteId) {
        return itemRepository.somarQuantidadeProdutoNoLote(produtoId, loteId, STATUS_QUE_OCUPAM_ESTOQUE);
    }

    /** Quantidade ainda disponivel do produto. Nulo = sem limite. */
    public Integer disponivel(Produto produto) {
        if (produto.getLimiteEstoque() == null) {
            return null;
        }
        long restante = produto.getLimiteEstoque() - vendidos(produto.getId());
        return (int) Math.max(0, restante);
    }

    /**
     * Escolhe o primeiro lote vigente que ainda tem quantidade.
     * Se nenhum lote vigente servir, usa o preco padrao do produto.
     */
    public PrecoVigente precoVigente(Produto produto, LocalDateTime agora) {
        for (LoteProduto lp : loteProdutoRepository.findVigentes(produto.getId(), agora)) {
            if (lp.getQuantidade() == null) {
                return new PrecoVigente(lp.getPrecoLote(), lp);
            }
            long restanteLote = lp.getQuantidade() - vendidosNoLote(produto.getId(), lp.getLote().getId());
            if (restanteLote > 0) {
                return new PrecoVigente(lp.getPrecoLote(), lp);
            }
        }
        return new PrecoVigente(produto.getPreco(), null);
    }

    /** Quantidade restante no lote. Nulo = sem limite no lote. */
    public Integer disponivelNoLote(LoteProduto lp) {
        if (lp == null || lp.getQuantidade() == null) {
            return null;
        }
        long restante = lp.getQuantidade() - vendidosNoLote(lp.getProduto().getId(), lp.getLote().getId());
        return (int) Math.max(0, restante);
    }
}
