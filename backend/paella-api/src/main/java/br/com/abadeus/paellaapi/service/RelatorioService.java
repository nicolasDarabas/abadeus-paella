package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Pagamento;
import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.entity.PedidoItem;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.entity.Retirada;
import br.com.abadeus.paellaapi.domain.enums.StatusPagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.repository.PagamentoRepository;
import br.com.abadeus.paellaapi.repository.PedidoRepository;
import br.com.abadeus.paellaapi.repository.ProdutoRepository;
import br.com.abadeus.paellaapi.repository.RetiradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Relatorios exportaveis em CSV (abrem no Excel / Google Planilhas).
 * Separador ";" e decimal com virgula, padrao brasileiro.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<StatusPedido> CONFIRMADOS = EnumSet.of(StatusPedido.PAGO, StatusPedido.RETIRADO);

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final RetiradaRepository retiradaRepository;
    private final EventoService eventoService;

    public RelatorioService(PedidoRepository pedidoRepository, PagamentoRepository pagamentoRepository,
                            ProdutoRepository produtoRepository, RetiradaRepository retiradaRepository,
                            EventoService eventoService) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.produtoRepository = produtoRepository;
        this.retiradaRepository = retiradaRepository;
        this.eventoService = eventoService;
    }

    /** Um pedido por linha: numero, cliente, status, pagamento, itens, total e retirada. */
    public String pedidosCsv(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        StringBuilder sb = new StringBuilder();
        linha(sb, "Numero", "Data", "Cliente", "E-mail", "Origem", "Status", "Metodo pagamento",
                "Status pagamento", "Itens", "Total (R$)", "Retirado em", "Validado por");
        for (Pedido p : pedidoRepository.findByEventoIdOrderByCriadoEmDesc(eventoId)) {
            List<Pagamento> pgs = pagamentoRepository.findByPedidoIdOrderByCriadoEmDesc(p.getId());
            Pagamento pg = pgs.isEmpty() ? null : pgs.get(0);
            StringBuilder itens = new StringBuilder();
            for (PedidoItem i : p.getItens()) {
                if (itens.length() > 0) {
                    itens.append(" | ");
                }
                itens.append(i.getQuantidade()).append("x ").append(i.getProduto().getNome());
            }
            Retirada r = retiradaRepository.findByPedidoId(p.getId()).orElse(null);
            linha(sb, p.getNumero(), data(p.getCriadoEm()), p.getUsuario().getNome(), p.getUsuario().getEmail(),
                    p.getOrigem().name(), p.getStatus().name(), pg == null ? "" : pg.getMetodo().name(),
                    pg == null ? "" : pg.getStatus().name(), itens.toString(), valor(p.getValorTotal()),
                    r == null ? "" : data(r.getValidadoEm()), r == null ? "" : r.getValidadoPor());
        }
        return sb.toString();
    }

    /** Arrecadacao por produto e por forma de pagamento, mais o resumo de retiradas. */
    public String arrecadacaoCsv(Long eventoId) {
        Evento evento = eventoService.buscarEntidade(eventoId);
        List<Pedido> pedidos = pedidoRepository.findByEventoIdOrderByCriadoEmDesc(eventoId);

        Map<Long, long[]> qtdPorProduto = new LinkedHashMap<>();
        Map<Long, BigDecimal> valorPorProduto = new LinkedHashMap<>();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal reembolsado = BigDecimal.ZERO;
        long retirados = 0;
        long aRetirar = 0;

        for (Pedido p : pedidos) {
            if (p.getStatus() == StatusPedido.REEMBOLSADO) {
                reembolsado = reembolsado.add(p.getValorTotal());
            }
            if (!CONFIRMADOS.contains(p.getStatus())) {
                continue;
            }
            if (p.getStatus() == StatusPedido.RETIRADO) {
                retirados++;
            } else {
                aRetirar++;
            }
            total = total.add(p.getValorTotal());
            for (PedidoItem i : p.getItens()) {
                Long id = i.getProduto().getId();
                qtdPorProduto.computeIfAbsent(id, k -> new long[1])[0] += i.getQuantidade();
                valorPorProduto.merge(id, i.getSubtotal(), BigDecimal::add);
            }
            for (Pagamento pg : pagamentoRepository.findByPedidoIdAndStatus(p.getId(), StatusPagamento.APROVADO)) {
                porMetodo.merge(pg.getMetodo().name(), pg.getValor(), BigDecimal::add);
            }
        }

        StringBuilder sb = new StringBuilder();
        linha(sb, "Relatorio de arrecadacao", evento.getTitulo());
        linha(sb, "Gerado em", data(LocalDateTime.now()));
        linha(sb);
        linha(sb, "Produto", "Unidades vendidas", "Limite de estoque", "Receita confirmada (R$)");
        for (Produto prod : produtoRepository.findByEventoIdOrderByNomeAsc(eventoId)) {
            long[] q = qtdPorProduto.get(prod.getId());
            linha(sb, prod.getNome(), String.valueOf(q == null ? 0 : q[0]),
                    prod.getLimiteEstoque() == null ? "sem limite" : String.valueOf(prod.getLimiteEstoque()),
                    valor(valorPorProduto.getOrDefault(prod.getId(), BigDecimal.ZERO)));
        }
        linha(sb);
        linha(sb, "Forma de pagamento", "Valor (R$)");
        for (Map.Entry<String, BigDecimal> e : porMetodo.entrySet()) {
            linha(sb, e.getKey(), valor(e.getValue()));
        }
        linha(sb);
        linha(sb, "Receita confirmada (R$)", valor(total));
        linha(sb, "Valor reembolsado (R$)", valor(reembolsado));
        linha(sb, "Pedidos retirados", String.valueOf(retirados));
        linha(sb, "Pedidos pagos a retirar", String.valueOf(aRetirar));
        return sb.toString();
    }

    // ------------------------------------------------------------------

    private static void linha(StringBuilder sb, String... colunas) {
        for (int i = 0; i < colunas.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            String c = colunas[i] == null ? "" : colunas[i];
            if (c.contains(";") || c.contains("\"") || c.contains("\n")) {
                c = "\"" + c.replace("\"", "\"\"") + "\"";
            }
            sb.append(c);
        }
        sb.append("\r\n");
    }

    private static String valor(BigDecimal v) {
        return v == null ? "0,00" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String data(LocalDateTime d) {
        return d == null ? "" : d.format(FMT);
    }
}
