package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.ComprovanteRetirada;
import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.entity.Retirada;
import br.com.abadeus.paellaapi.domain.enums.MetodoValidacao;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.domain.enums.TipoNotificacao;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.ConsultaRetiradaResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.PendenteRetiradaResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.ResultadoSync;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.RetiradaResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.SyncRetiradaItem;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.SyncResultado;
import br.com.abadeus.paellaapi.exception.ConflitoException;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.exception.RegraNegocioException;
import br.com.abadeus.paellaapi.repository.ComprovanteRetiradaRepository;
import br.com.abadeus.paellaapi.repository.PedidoRepository;
import br.com.abadeus.paellaapi.repository.RetiradaRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Retirada da paella no dia do evento.
 * Regra principal: RETIRADA UNICA (pedido_id e UNIQUE na tabela retirada).
 */
@Service
@Transactional(readOnly = true)
public class RetiradaService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm");

    private final PedidoRepository pedidoRepository;
    private final RetiradaRepository retiradaRepository;
    private final ComprovanteRetiradaRepository comprovanteRepository;
    private final HistoricoService historicoService;
    private final PedidoMapper mapper;
    private final NotificacaoService notificacaoService;

    public RetiradaService(PedidoRepository pedidoRepository, RetiradaRepository retiradaRepository,
                           ComprovanteRetiradaRepository comprovanteRepository, HistoricoService historicoService,
                           PedidoMapper mapper, NotificacaoService notificacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.retiradaRepository = retiradaRepository;
        this.comprovanteRepository = comprovanteRepository;
        this.historicoService = historicoService;
        this.mapper = mapper;
        this.notificacaoService = notificacaoService;
    }

    /** Pre-visualizacao apos ler o QR: mostra o pedido e se pode ser entregue. */
    public ConsultaRetiradaResponse consultar(String codigo) {
        Pedido p = localizar(codigo).orElseThrow(() -> new NaoEncontradoException("Codigo nao corresponde a nenhum pedido."));
        String mensagem;
        boolean pode = false;
        switch (p.getStatus()) {
            case PAGO -> {
                pode = true;
                mensagem = "Pedido pago. Pode entregar.";
            }
            case RETIRADO -> mensagem = mensagemJaRetirado(p);
            case AGUARDANDO_PAGAMENTO -> mensagem = "Pedido ainda aguardando pagamento.";
            default -> mensagem = "Pedido " + p.getStatus() + ". Nao entregar.";
        }
        return new ConsultaRetiradaResponse(p.getId(), p.getNumero(), p.getUsuario().getNome(), p.getStatus(),
                pode, mensagem, mapper.itens(p));
    }

    @Transactional
    public RetiradaResponse validar(String codigo, MetodoValidacao metodo) {
        Pedido p = localizar(codigo).orElseThrow(() -> new NaoEncontradoException("Codigo nao corresponde a nenhum pedido."));
        MetodoValidacao m = metodo != null ? metodo
                : (codigo.trim().equalsIgnoreCase(p.getNumero()) ? MetodoValidacao.CODIGO_MANUAL : MetodoValidacao.QR_CODE);
        return paraResponse(registrar(p, m, LocalDateTime.now(), false));
    }

    public List<RetiradaResponse> listarPorEvento(Long eventoId) {
        return retiradaRepository.findByPedidoEventoIdOrderByValidadoEmDesc(eventoId).stream()
                .map(this::paraResponse).toList();
    }

    // ---------- Modo contingencia (app sem internet) ----------

    /** Lista de pedidos pagos e nao retirados, para o app guardar no banco local. */
    public List<PendenteRetiradaResponse> pendentes(Long eventoId) {
        return pedidoRepository.findByEventoIdAndStatus(eventoId, StatusPedido.PAGO).stream()
                .map(p -> new PendenteRetiradaResponse(p.getId(), p.getNumero(), p.getCodigoRetirada(),
                        p.getUsuario().getNome(), mapper.itens(p)))
                .toList();
    }

    /** Recebe as retiradas feitas offline e grava no servidor, item a item. */
    @Transactional
    public List<SyncResultado> sincronizar(List<SyncRetiradaItem> itens) {
        List<SyncResultado> resultados = new ArrayList<>();
        for (SyncRetiradaItem item : itens) {
            Optional<Pedido> opt = localizar(item.codigo());
            if (opt.isEmpty()) {
                resultados.add(new SyncResultado(item.codigo(), ResultadoSync.INVALIDO, "Codigo nao encontrado"));
                continue;
            }
            Pedido p = opt.get();
            if (p.getStatus() == StatusPedido.RETIRADO) {
                resultados.add(new SyncResultado(item.codigo(), ResultadoSync.JA_RETIRADO, mensagemJaRetirado(p)));
                continue;
            }
            if (p.getStatus() != StatusPedido.PAGO) {
                resultados.add(new SyncResultado(item.codigo(), ResultadoSync.INVALIDO,
                        "Pedido com status " + p.getStatus()));
                continue;
            }
            LocalDateTime quando = item.validadoEm() != null ? item.validadoEm() : LocalDateTime.now();
            registrar(p, MetodoValidacao.OFFLINE_SINCRONIZADO, quando, true);
            resultados.add(new SyncResultado(item.codigo(), ResultadoSync.OK, "Retirada registrada"));
        }
        return resultados;
    }

    // ------------------------------------------------------------------

    private Retirada registrar(Pedido p, MetodoValidacao metodo, LocalDateTime quando, boolean offline) {
        if (p.getStatus() == StatusPedido.RETIRADO || retiradaRepository.existsByPedidoId(p.getId())) {
            throw new ConflitoException(mensagemJaRetirado(p));
        }
        if (p.getStatus() != StatusPedido.PAGO) {
            throw new RegraNegocioException("Pedido com status " + p.getStatus() + " nao pode ser retirado.");
        }
        String validador = SecurityUtils.loginAtualOuSistema();

        Retirada r = new Retirada();
        r.setPedido(p);
        r.setValidadoPor(validador);
        r.setValidadoEm(quando);
        r.setMetodoValidacao(metodo);
        r.setSincronizadoOffline(offline);
        retiradaRepository.save(r);

        ComprovanteRetirada c = new ComprovanteRetirada();
        c.setRetirada(r);
        c.setCodigo("CR-" + CodigoGenerator.gerar(8));
        c.setEmitidoEm(LocalDateTime.now());
        comprovanteRepository.save(c);

        historicoService.alterarStatus(p, StatusPedido.RETIRADO, "Retirada validada (" + metodo + ")");
        notificacaoService.avisar(p, TipoNotificacao.RETIRADA_REALIZADA);
        return r;
    }

    /** Aceita o codigo do QR (ABD-...) ou o numero do pedido (PAE-...). */
    private Optional<Pedido> localizar(String codigo) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase();
        Optional<Pedido> p = pedidoRepository.findByCodigoRetirada(c);
        return p.isPresent() ? p : pedidoRepository.findByNumero(c);
    }

    private String mensagemJaRetirado(Pedido p) {
        return retiradaRepository.findByPedidoId(p.getId())
                .map(r -> "Pedido JA RETIRADO em " + r.getValidadoEm().format(FMT) + " (validado por " + r.getValidadoPor() + ").")
                .orElse("Pedido ja retirado.");
    }

    private RetiradaResponse paraResponse(Retirada r) {
        Pedido p = r.getPedido();
        String comprovante = comprovanteRepository.findByRetiradaId(r.getId())
                .map(ComprovanteRetirada::getCodigo).orElse(null);
        return new RetiradaResponse(r.getId(), p.getId(), p.getNumero(), p.getUsuario().getNome(),
                r.getValidadoPor(), r.getValidadoEm(), r.getMetodoValidacao(), r.isSincronizadoOffline(),
                comprovante, mapper.itens(p));
    }
}
