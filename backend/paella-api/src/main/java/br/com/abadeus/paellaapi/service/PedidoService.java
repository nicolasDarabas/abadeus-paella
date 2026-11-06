package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Pagamento;
import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.entity.PedidoItem;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.entity.Usuario;
import br.com.abadeus.paellaapi.domain.enums.OrigemPedido;
import br.com.abadeus.paellaapi.domain.enums.StatusEvento;
import br.com.abadeus.paellaapi.domain.enums.StatusPagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.domain.enums.TipoNotificacao;
import br.com.abadeus.paellaapi.dto.PedidoDtos.ComprovanteResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.CriarPedidoRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.HistoricoResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.ItemRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoResponse;
import br.com.abadeus.paellaapi.exception.AcessoNegadoException;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.exception.RegraNegocioException;
import br.com.abadeus.paellaapi.repository.PagamentoRepository;
import br.com.abadeus.paellaapi.repository.PedidoRepository;
import br.com.abadeus.paellaapi.repository.ProdutoRepository;
import br.com.abadeus.paellaapi.repository.UsuarioRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import br.com.abadeus.paellaapi.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagamentoRepository pagamentoRepository;
    private final EventoService eventoService;
    private final EstoqueService estoqueService;
    private final HistoricoService historicoService;
    private final PedidoMapper mapper;
    private final QrCodeService qrCodeService;
    private final NotificacaoService notificacaoService;
    private final long minutosReserva;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
                         UsuarioRepository usuarioRepository, PagamentoRepository pagamentoRepository,
                         EventoService eventoService, EstoqueService estoqueService,
                         HistoricoService historicoService, PedidoMapper mapper, QrCodeService qrCodeService,
                         NotificacaoService notificacaoService,
                         @Value("${app.pedido.minutos-reserva:30}") long minutosReserva) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.eventoService = eventoService;
        this.estoqueService = estoqueService;
        this.historicoService = historicoService;
        this.mapper = mapper;
        this.qrCodeService = qrCodeService;
        this.notificacaoService = notificacaoService;
        this.minutosReserva = minutosReserva;
    }

    // ------------------------------------------------------------------
    // Criacao
    // ------------------------------------------------------------------

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest req) {
        UsuarioAutenticado logado = SecurityUtils.usuarioAtual();
        Usuario usuario = usuarioRepository.findById(logado.id())
                .orElseThrow(() -> new NaoEncontradoException("Usuario", logado.id()));

        OrigemPedido origem = req.origem() == null ? OrigemPedido.APP : req.origem();
        if (origem == OrigemPedido.BALCAO && !logado.isAdministrador()) {
            throw new AcessoNegadoException("Somente o administrador pode registrar vendas no balcao.");
        }

        Evento evento = eventoService.buscarEntidade(req.eventoId());
        if (evento.getStatus() != StatusEvento.VENDAS_ABERTAS) {
            throw new RegraNegocioException("As vendas deste evento nao estao abertas.");
        }

        // Junta itens repetidos do mesmo produto
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemRequest item : req.itens()) {
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        LocalDateTime agora = LocalDateTime.now();
        Pedido pedido = new Pedido();
        pedido.setEvento(evento);
        pedido.setUsuario(usuario);
        pedido.setOrigem(origem);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setObservacao(req.observacao());
        pedido.setNumero(gerarNumero());
        pedido.setExpiraEm(agora.plusMinutes(minutosReserva));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : quantidades.entrySet()) {
            Produto produto = produtoRepository.findById(e.getKey())
                    .orElseThrow(() -> new NaoEncontradoException("Produto", e.getKey()));
            int qtd = e.getValue();
            validarProduto(produto, evento, qtd);

            EstoqueService.PrecoVigente pv = estoqueService.precoVigente(produto, agora);
            Integer noLote = estoqueService.disponivelNoLote(pv.loteProduto());
            if (noLote != null && noLote < qtd) {
                throw new RegraNegocioException("Restam apenas " + noLote + " unidade(s) de '" + produto.getNome()
                        + "' no lote " + pv.loteProduto().getLote().getNome() + ".");
            }

            PedidoItem item = new PedidoItem();
            item.setProduto(produto);
            item.setLote(pv.loteProduto() == null ? null : pv.loteProduto().getLote());
            item.setQuantidade(qtd);
            item.setPrecoUnitario(pv.preco());
            item.setSubtotal(pv.preco().multiply(BigDecimal.valueOf(qtd)));
            pedido.adicionarItem(item);
            total = total.add(item.getSubtotal());
        }
        pedido.setValorTotal(total);

        pedidoRepository.save(pedido);
        // status inicial registrado no historico (anterior = nulo)
        pedido.setStatus(null);
        historicoService.alterarStatus(pedido, StatusPedido.AGUARDANDO_PAGAMENTO, "Pedido criado via " + origem);
        notificacaoService.avisar(pedido, TipoNotificacao.PEDIDO_CRIADO);
        return mapper.paraResponse(pedido);
    }

    private void validarProduto(Produto produto, Evento evento, int qtd) {
        if (!produto.getEvento().getId().equals(evento.getId())) {
            throw new RegraNegocioException("O produto '" + produto.getNome() + "' nao pertence a este evento.");
        }
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("O produto '" + produto.getNome() + "' nao esta disponivel.");
        }
        Integer disponivel = estoqueService.disponivel(produto);
        if (disponivel != null && disponivel < qtd) {
            throw new RegraNegocioException(disponivel == 0
                    ? "'" + produto.getNome() + "' esgotado."
                    : "Restam apenas " + disponivel + " unidade(s) de '" + produto.getNome() + "'.");
        }
    }

    private String gerarNumero() {
        String numero;
        do {
            numero = "PAE-" + CodigoGenerator.gerar(6);
        } while (pedidoRepository.existsByNumero(numero));
        return numero;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<PedidoResponse> meusPedidos() {
        Long id = SecurityUtils.usuarioAtual().id();
        return pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc(id).stream().map(mapper::paraResponse).toList();
    }

    public PedidoResponse buscar(Long id) {
        return mapper.paraResponse(buscarComPermissao(id));
    }

    /** Lista para o administrador, com filtros opcionais. */
    public List<PedidoResponse> listarAdmin(Long eventoId, StatusPedido status) {
        List<Pedido> pedidos;
        if (eventoId != null && status != null) {
            pedidos = pedidoRepository.findByEventoIdAndStatusOrderByCriadoEmDesc(eventoId, status);
        } else if (eventoId != null) {
            pedidos = pedidoRepository.findByEventoIdOrderByCriadoEmDesc(eventoId);
        } else if (status != null) {
            pedidos = pedidoRepository.findByStatusOrderByCriadoEmDesc(status);
        } else {
            pedidos = pedidoRepository.findAllByOrderByCriadoEmDesc();
        }
        return pedidos.stream().map(mapper::paraResponse).toList();
    }

    public List<HistoricoResponse> historico(Long id) {
        buscarComPermissao(id);
        return historicoService.listar(id);
    }

    /** Comprovante com QR Code: so existe depois do pagamento. */
    public ComprovanteResponse comprovante(Long id) {
        Pedido p = buscarComPermissao(id);
        exigirQrDisponivel(p);
        Evento e = p.getEvento();
        return new ComprovanteResponse(p.getId(), p.getNumero(), p.getCodigoRetirada(),
                qrCodeService.gerarBase64(p.getCodigoRetirada(), 400), e.getTitulo(), e.getLocal(),
                e.getDataEvento(), e.getRetiradaInicio(), e.getRetiradaFim(), p.getUsuario().getNome(),
                p.getStatus(), p.getValorTotal(), mapper.itens(p));
    }

    public byte[] qrCodePng(Long id) {
        Pedido p = buscarComPermissao(id);
        exigirQrDisponivel(p);
        return qrCodeService.gerarPng(p.getCodigoRetirada(), 400);
    }

    private void exigirQrDisponivel(Pedido p) {
        if (p.getStatus() != StatusPedido.PAGO && p.getStatus() != StatusPedido.RETIRADO) {
            throw new RegraNegocioException("O QR Code fica disponivel somente apos a confirmacao do pagamento.");
        }
    }

    // ------------------------------------------------------------------
    // Cancelamento e expiracao
    // ------------------------------------------------------------------

    @Transactional
    public PedidoResponse cancelar(Long id, String motivo) {
        Pedido p = buscarComPermissao(id);
        boolean admin = SecurityUtils.usuarioAtual().isAdministrador();
        if (admin && p.getStatus() == StatusPedido.PAGO) {
            // pedido ja pago: o cancelamento vira reembolso
            return reembolsar(id, motivo);
        }
        if (p.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
            cancelarPagamentosPendentes(p);
            historicoService.alterarStatus(p, StatusPedido.CANCELADO,
                    motivo == null || motivo.isBlank() ? "Cancelado pelo usuario" : motivo);
            p.setExpiraEm(null);
            notificacaoService.avisar(p, TipoNotificacao.PEDIDO_CANCELADO);
            return mapper.paraResponse(p);
        }
        throw new RegraNegocioException(p.getStatus() == StatusPedido.PAGO
                ? "Pedido ja pago: solicite o reembolso a organizacao."
                : "Nao e possivel cancelar um pedido com status " + p.getStatus() + ".");
    }

    /**
     * Reembolso (somente administrador): pedido PAGO e ainda nao retirado.
     * O pagamento aprovado passa a REEMBOLSADO e o estoque e liberado.
     * Com integracao real, aqui seria chamada a API de estorno do provedor.
     */
    @Transactional
    public PedidoResponse reembolsar(Long id, String motivo) {
        Pedido p = pedidoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido", id));
        if (p.getStatus() != StatusPedido.PAGO) {
            throw new RegraNegocioException(p.getStatus() == StatusPedido.RETIRADO
                    ? "Pedido ja retirado nao pode ser reembolsado."
                    : "Somente pedidos pagos podem ser reembolsados (status atual: " + p.getStatus() + ").");
        }
        for (Pagamento pg : pagamentoRepository.findByPedidoIdAndStatus(p.getId(), StatusPagamento.APROVADO)) {
            pg.setStatus(StatusPagamento.REEMBOLSADO);
        }
        historicoService.alterarStatus(p, StatusPedido.REEMBOLSADO,
                motivo == null || motivo.isBlank() ? "Reembolsado pela organizacao" : motivo);
        notificacaoService.avisar(p, TipoNotificacao.PEDIDO_REEMBOLSADO);
        return mapper.paraResponse(p);
    }

    /** Libera o estoque de pedidos nao pagos dentro do prazo (roda a cada minuto). */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void expirarReservas() {
        List<Pedido> vencidos = pedidoRepository.findByStatusAndExpiraEmBefore(
                StatusPedido.AGUARDANDO_PAGAMENTO, LocalDateTime.now());
        for (Pedido p : vencidos) {
            cancelarPagamentosPendentes(p);
            historicoService.alterarStatus(p, StatusPedido.EXPIRADO, "Prazo de pagamento esgotado");
            notificacaoService.avisar(p, TipoNotificacao.PEDIDO_EXPIRADO);
        }
        if (!vencidos.isEmpty()) {
            log.info("{} pedido(s) expirado(s) por falta de pagamento", vencidos.size());
        }
    }

    public void cancelarPagamentosPendentes(Pedido p) {
        for (Pagamento pg : pagamentoRepository.findByPedidoIdAndStatus(p.getId(), StatusPagamento.PENDENTE)) {
            pg.setStatus(StatusPagamento.CANCELADO);
        }
    }

    // ------------------------------------------------------------------

    /** Comprador so acessa os proprios pedidos; administrador acessa todos. */
    public Pedido buscarComPermissao(Long id) {
        Pedido p = pedidoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido", id));
        UsuarioAutenticado u = SecurityUtils.usuarioAtual();
        if (!u.isAdministrador() && !p.getUsuario().getId().equals(u.id())) {
            throw new AcessoNegadoException("Este pedido pertence a outro usuario.");
        }
        return p;
    }
}
