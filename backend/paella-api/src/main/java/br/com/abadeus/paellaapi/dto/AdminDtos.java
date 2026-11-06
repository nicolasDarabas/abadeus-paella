package br.com.abadeus.paellaapi.dto;

import br.com.abadeus.paellaapi.domain.enums.StatusPedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record ProdutoVendidoResponse(Long produtoId, String nome, long vendidos, Integer limiteEstoque,
                                         Integer disponivel) {
    }

    /** Painel do administrador para um evento. */
    public record ResumoEventoResponse(Long eventoId, String titulo, long totalPedidos,
                                       Map<StatusPedido, Long> pedidosPorStatus,
                                       BigDecimal valorArrecadado, BigDecimal valorAguardandoPagamento,
                                       long retirados, long aRetirar,
                                       List<ProdutoVendidoResponse> produtos) {
    }
}
