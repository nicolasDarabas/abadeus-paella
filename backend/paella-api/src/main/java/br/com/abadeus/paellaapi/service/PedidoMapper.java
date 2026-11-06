package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Pagamento;
import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.entity.PedidoItem;
import br.com.abadeus.paellaapi.domain.entity.Retirada;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PagamentoResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoItemResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoResponse;
import br.com.abadeus.paellaapi.repository.PagamentoRepository;
import br.com.abadeus.paellaapi.repository.RetiradaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Converte entidades de pedido em respostas JSON. Usar dentro de uma transacao. */
@Component
public class PedidoMapper {

    private final PagamentoRepository pagamentoRepository;
    private final RetiradaRepository retiradaRepository;

    public PedidoMapper(PagamentoRepository pagamentoRepository, RetiradaRepository retiradaRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.retiradaRepository = retiradaRepository;
    }

    public PedidoResponse paraResponse(Pedido p) {
        List<PagamentoResponse> pagamentos = pagamentoRepository.findByPedidoIdOrderByCriadoEmDesc(p.getId())
                .stream().map(PedidoMapper::pagamento).toList();
        var retiradoEm = retiradaRepository.findByPedidoId(p.getId()).map(Retirada::getValidadoEm).orElse(null);
        boolean qrDisponivel = p.getStatus() == StatusPedido.PAGO || p.getStatus() == StatusPedido.RETIRADO;
        return new PedidoResponse(p.getId(), p.getNumero(), p.getEvento().getId(), p.getEvento().getTitulo(),
                p.getUsuario().getId(), p.getUsuario().getNome(), p.getOrigem(), p.getStatus(), p.getValorTotal(),
                p.getExpiraEm(), p.getCriadoEm(), p.getObservacao(), qrDisponivel, retiradoEm,
                itens(p), pagamentos);
    }

    public List<PedidoItemResponse> itens(Pedido p) {
        return p.getItens().stream().map(PedidoMapper::item).toList();
    }

    public static PedidoItemResponse item(PedidoItem i) {
        return new PedidoItemResponse(i.getProduto().getId(), i.getProduto().getNome(),
                i.getLote() == null ? null : i.getLote().getNome(),
                i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal());
    }

    public static PagamentoResponse pagamento(Pagamento pg) {
        return new PagamentoResponse(pg.getId(), pg.getPedido().getId(), pg.getMetodo(), pg.getValor(),
                pg.getStatus(), pg.getCodigoPix(), pg.getIdExterno(), pg.getConfirmadoEm(), pg.getConfirmadoPor(),
                pg.getCriadoEm());
    }
}
