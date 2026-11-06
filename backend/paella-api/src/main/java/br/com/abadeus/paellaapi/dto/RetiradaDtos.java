package br.com.abadeus.paellaapi.dto;

import br.com.abadeus.paellaapi.domain.enums.MetodoValidacao;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public final class RetiradaDtos {

    private RetiradaDtos() {
    }

    /**
     * codigo = conteudo lido do QR Code (codigo de retirada) ou o numero do pedido (ex.: PAE-7KQ2XM).
     */
    public record ValidarRetiradaRequest(@NotBlank(message = "Informe o codigo") String codigo,
                                         MetodoValidacao metodo) {
    }

    public record ConsultaRetiradaResponse(Long pedidoId, String numero, String cliente, StatusPedido status,
                                           boolean podeRetirar, String mensagem,
                                           List<PedidoDtos.PedidoItemResponse> itens) {
    }

    public record RetiradaResponse(Long retiradaId, Long pedidoId, String numeroPedido, String cliente,
                                   String validadoPor, LocalDateTime validadoEm, MetodoValidacao metodoValidacao,
                                   boolean sincronizadoOffline, String comprovanteCodigo,
                                   List<PedidoDtos.PedidoItemResponse> itens) {
    }

    // ---------- Modo contingencia (sem internet) ----------

    /** Pedido pago, ainda nao retirado: o app guarda essa lista no banco local. */
    public record PendenteRetiradaResponse(Long pedidoId, String numero, String codigoRetirada, String cliente,
                                           List<PedidoDtos.PedidoItemResponse> itens) {
    }

    public record SyncRetiradaItem(@NotBlank String codigo, LocalDateTime validadoEm) {
    }

    public record SyncRetiradasRequest(@NotEmpty @Valid List<SyncRetiradaItem> retiradas) {
    }

    public enum ResultadoSync { OK, JA_RETIRADO, INVALIDO }

    public record SyncResultado(String codigo, ResultadoSync resultado, String mensagem) {
    }
}
