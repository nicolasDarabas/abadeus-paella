package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Notificacao;
import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.domain.enums.TipoNotificacao;
import br.com.abadeus.paellaapi.dto.PedidoDtos.NotificacaoResponse;
import br.com.abadeus.paellaapi.repository.NotificacaoRepository;
import br.com.abadeus.paellaapi.repository.PedidoRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Comunicacao com o comprador.
 * O envio de e-mail e SIMULADO: cada aviso e gravado na tabela "notificacao"
 * e impresso no console. Para envio real, basta trocar o metodo "entregar"
 * por um provedor de e-mail (ex.: spring-boot-starter-mail).
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM '\u00e0s' HH:mm");

    private final NotificacaoRepository repository;
    private final PedidoRepository pedidoRepository;

    public NotificacaoService(NotificacaoRepository repository, PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public void avisar(Pedido p, TipoNotificacao tipo) {
        String numero = p.getNumero();
        String nome = p.getUsuario().getNome().split(" ")[0];
        String assunto;
        String texto;
        switch (tipo) {
            case PEDIDO_CRIADO -> {
                assunto = "Recebemos seu pedido " + numero;
                texto = "Ol\u00e1, " + nome + "! Seu pedido " + numero + " foi registrado. Conclua o pagamento em at\u00e9 30 minutos para garantir a reserva.";
            }
            case PAGAMENTO_CONFIRMADO -> {
                assunto = "Pagamento confirmado - pedido " + numero;
                texto = "Obrigado por apoiar a ABADEUS, " + nome + "! O pagamento do pedido " + numero
                        + " foi confirmado. Seu QR Code de retirada est\u00e1 dispon\u00edvel em Meus pedidos (c\u00f3digo "
                        + p.getCodigoRetirada() + ")." + janela(p);
            }
            case LEMBRETE_RETIRADA -> {
                assunto = "Lembrete: retire sua paella";
                texto = "Ol\u00e1, " + nome + "! Lembre-se de retirar o pedido " + numero + "." + janela(p)
                        + " Apresente o QR Code no celular ou o n\u00famero do pedido.";
            }
            case RETIRADA_REALIZADA -> {
                assunto = "Pedido " + numero + " retirado";
                texto = "Seu pedido " + numero + " foi entregue. Bom apetite e obrigado por ajudar a ABADEUS!";
            }
            case PEDIDO_CANCELADO -> {
                assunto = "Pedido " + numero + " cancelado";
                texto = "O pedido " + numero + " foi cancelado e os itens voltaram ao estoque.";
            }
            case PEDIDO_EXPIRADO -> {
                assunto = "Pedido " + numero + " expirado";
                texto = "O prazo de pagamento do pedido " + numero + " terminou e a reserva foi liberada. Voc\u00ea pode fazer um novo pedido pelo app ou site.";
            }
            case PEDIDO_REEMBOLSADO -> {
                assunto = "Reembolso do pedido " + numero;
                texto = "O pedido " + numero + " foi reembolsado pela organiza\u00e7\u00e3o. O valor ser\u00e1 devolvido pelo mesmo meio de pagamento.";
            }
            default -> {
                assunto = "Pedido " + numero;
                texto = "Atualiza\u00e7\u00e3o do pedido " + numero + ".";
            }
        }

        Notificacao n = new Notificacao();
        n.setUsuario(p.getUsuario());
        n.setPedido(p);
        n.setTipo(tipo);
        n.setCanal("EMAIL");
        n.setDestinatario(p.getUsuario().getEmail());
        n.setAssunto(assunto);
        n.setMensagem(texto);
        n.setEnviadaEm(LocalDateTime.now());
        n.setStatus("SIMULADO");
        repository.save(n);
        entregar(n);
    }

    private void entregar(Notificacao n) {
        log.info("[E-MAIL SIMULADO] Para: {} | Assunto: {} | {}", n.getDestinatario(), n.getAssunto(), n.getMensagem());
    }

    private static String janela(Pedido p) {
        if (p.getEvento().getRetiradaInicio() == null) {
            return "";
        }
        String s = " Retirada em " + p.getEvento().getRetiradaInicio().format(FMT);
        if (p.getEvento().getRetiradaFim() != null) {
            s += " at\u00e9 " + p.getEvento().getRetiradaFim().toLocalTime();
        }
        return s + " - " + p.getEvento().getLocal() + ".";
    }

    /** Lembrete automatico: pedidos pagos cuja retirada comeca nas proximas 24 horas (uma vez por pedido). */
    @Scheduled(fixedDelay = 15 * 60_000, initialDelay = 60_000)
    @Transactional
    public void enviarLembretes() {
        LocalDateTime agora = LocalDateTime.now();
        for (Pedido p : pedidoRepository.findByStatusOrderByCriadoEmDesc(StatusPedido.PAGO)) {
            LocalDateTime inicio = p.getEvento().getRetiradaInicio();
            if (inicio != null && inicio.isAfter(agora) && inicio.isBefore(agora.plusHours(24))
                    && !repository.existsByPedidoIdAndTipo(p.getId(), TipoNotificacao.LEMBRETE_RETIRADA)) {
                avisar(p, TipoNotificacao.LEMBRETE_RETIRADA);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> minhas() {
        Long usuarioId = SecurityUtils.usuarioAtual().id();
        return repository.findByUsuarioIdOrderByEnviadaEmDesc(usuarioId).stream().map(NotificacaoService::resp).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> todas() {
        return repository.findAllByOrderByEnviadaEmDesc().stream().map(NotificacaoService::resp).toList();
    }

    private static NotificacaoResponse resp(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getPedido() == null ? null : n.getPedido().getId(),
                n.getPedido() == null ? null : n.getPedido().getNumero(), n.getTipo().name(), n.getDestinatario(),
                n.getAssunto(), n.getMensagem(), n.getEnviadaEm(), n.getStatus());
    }
}
