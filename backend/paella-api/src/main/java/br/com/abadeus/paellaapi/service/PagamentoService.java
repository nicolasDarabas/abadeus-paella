package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Pagamento;
import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.enums.MetodoPagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.domain.enums.TipoNotificacao;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PagamentoResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoResponse;
import br.com.abadeus.paellaapi.exception.AcessoNegadoException;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.exception.RegraNegocioException;
import br.com.abadeus.paellaapi.repository.PagamentoRepository;
import br.com.abadeus.paellaapi.repository.PedidoRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import br.com.abadeus.paellaapi.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pagamento SIMULADO (projeto academico).
 * - PIX / CARTAO: o proprio comprador pode "simular a aprovacao" (substitui o retorno do banco).
 * - DINHEIRO: somente o administrador confirma, no caixa.
 */
@Service
@Transactional(readOnly = true)
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final HistoricoService historicoService;
    private final PedidoMapper mapper;
    private final NotificacaoService notificacaoService;

    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoRepository pedidoRepository,
                            PedidoService pedidoService, HistoricoService historicoService, PedidoMapper mapper,
                            NotificacaoService notificacaoService) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
        this.historicoService = historicoService;
        this.mapper = mapper;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public PagamentoResponse iniciar(Long pedidoId, MetodoPagamento metodo) {
        Pedido pedido = pedidoService.buscarComPermissao(pedidoId);
        exigirAguardandoPagamento(pedido);

        // Um pedido tem apenas um pagamento pendente por vez
        pedidoService.cancelarPagamentosPendentes(pedido);

        Pagamento pg = new Pagamento();
        pg.setPedido(pedido);
        pg.setMetodo(metodo);
        pg.setValor(pedido.getValorTotal());
        pg.setStatus(StatusPagamento.PENDENTE);
        pg.setIdExterno("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (metodo == MetodoPagamento.PIX) {
            pg.setCodigoPix(gerarPixCopiaECola(pedido));
        }
        return PedidoMapper.pagamento(pagamentoRepository.save(pg));
    }

    public List<PagamentoResponse> listarPorPedido(Long pedidoId) {
        pedidoService.buscarComPermissao(pedidoId);
        return pagamentoRepository.findByPedidoIdOrderByCriadoEmDesc(pedidoId).stream()
                .map(PedidoMapper::pagamento).toList();
    }

    /** Simula o retorno do banco/operadora aprovando PIX ou CARTAO. */
    @Transactional
    public PedidoResponse simularAprovacao(Long pagamentoId) {
        Pagamento pg = buscar(pagamentoId);
        UsuarioAutenticado u = SecurityUtils.usuarioAtual();
        if (!u.isAdministrador() && !pg.getPedido().getUsuario().getId().equals(u.id())) {
            throw new AcessoNegadoException("Este pagamento pertence a outro usuario.");
        }
        if (pg.getMetodo() == MetodoPagamento.DINHEIRO && !u.isAdministrador()) {
            throw new AcessoNegadoException("Pagamento em dinheiro e confirmado somente pela organizacao, no caixa.");
        }
        return aprovar(pg);
    }

    /** Confirmacao manual pelo administrador (qualquer metodo, principalmente DINHEIRO). */
    @Transactional
    public PedidoResponse confirmar(Long pagamentoId) {
        return aprovar(buscar(pagamentoId));
    }

    @Transactional
    public PagamentoResponse recusar(Long pagamentoId) {
        Pagamento pg = buscar(pagamentoId);
        if (pg.getStatus() != StatusPagamento.PENDENTE) {
            throw new RegraNegocioException("Somente pagamentos pendentes podem ser recusados.");
        }
        pg.setStatus(StatusPagamento.RECUSADO);
        pg.setConfirmadoEm(LocalDateTime.now());
        pg.setConfirmadoPor(SecurityUtils.loginAtualOuSistema());
        return PedidoMapper.pagamento(pg);
    }

    private PedidoResponse aprovar(Pagamento pg) {
        if (pg.getStatus() != StatusPagamento.PENDENTE) {
            throw new RegraNegocioException("Este pagamento ja foi processado (status " + pg.getStatus() + ").");
        }
        Pedido pedido = pg.getPedido();
        exigirAguardandoPagamento(pedido);

        pg.setStatus(StatusPagamento.APROVADO);
        pg.setConfirmadoEm(LocalDateTime.now());
        pg.setConfirmadoPor(SecurityUtils.loginAtualOuSistema());

        pedido.setCodigoRetirada(gerarCodigoRetirada());
        pedido.setExpiraEm(null);
        historicoService.alterarStatus(pedido, StatusPedido.PAGO, "Pagamento " + pg.getMetodo() + " aprovado");
        notificacaoService.avisar(pedido, TipoNotificacao.PAGAMENTO_CONFIRMADO);
        return mapper.paraResponse(pedido);
    }

    private void exigirAguardandoPagamento(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("O pedido nao esta aguardando pagamento (status " + pedido.getStatus() + ").");
        }
        if (pedido.getExpiraEm() != null && pedido.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("O prazo de pagamento deste pedido expirou. Faca um novo pedido.");
        }
    }

    private Pagamento buscar(Long id) {
        return pagamentoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Pagamento", id));
    }

    /** Codigo unico que vai dentro do QR Code. */
    private String gerarCodigoRetirada() {
        String codigo;
        do {
            codigo = "ABD-" + CodigoGenerator.gerar(10);
        } while (pedidoRepository.findByCodigoRetirada(codigo).isPresent());
        return codigo;
    }

    /** Texto ilustrativo no formato de um "Pix copia e cola" (NAO e um Pix real). */
    private String gerarPixCopiaECola(Pedido pedido) {
        return "00020126SIMULADO-ABADEUS-PAELLA-" + pedido.getNumero() + "-VALOR-"
                + pedido.getValorTotal().toPlainString() + "-" + CodigoGenerator.gerar(8);
    }
}
