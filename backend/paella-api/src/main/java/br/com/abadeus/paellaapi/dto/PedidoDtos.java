package br.com.abadeus.paellaapi.dto;

import br.com.abadeus.paellaapi.domain.enums.MetodoPagamento;
import br.com.abadeus.paellaapi.domain.enums.OrigemPedido;
import br.com.abadeus.paellaapi.domain.enums.StatusPagamento;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDtos {

    private PedidoDtos() {
    }

    public record ItemRequest(
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe a quantidade") @Min(value = 1, message = "Quantidade minima: 1")
            @Max(value = 50, message = "Quantidade maxima por item: 50") Integer quantidade) {
    }

    public record CriarPedidoRequest(
            @NotNull(message = "Informe o evento") Long eventoId,
            OrigemPedido origem,
            @NotEmpty(message = "Inclua pelo menos um item") @Valid List<ItemRequest> itens,
            @Size(max = 500) String observacao) {
    }

    public record CancelarRequest(@Size(max = 300) String motivo) {
    }

    public record PedidoItemResponse(Long produtoId, String produtoNome, String loteNome, int quantidade,
                                     BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    public record PagamentoResponse(Long id, Long pedidoId, MetodoPagamento metodo, BigDecimal valor,
                                    StatusPagamento status, String codigoPix, String idExterno,
                                    LocalDateTime confirmadoEm, String confirmadoPor, LocalDateTime criadoEm) {
    }

    public record PedidoResponse(Long id, String numero, Long eventoId, String eventoTitulo,
                                 Long usuarioId, String usuarioNome, OrigemPedido origem, StatusPedido status,
                                 BigDecimal valorTotal, LocalDateTime expiraEm, LocalDateTime criadoEm,
                                 String observacao, boolean qrCodeDisponivel, LocalDateTime retiradoEm,
                                 List<PedidoItemResponse> itens, List<PagamentoResponse> pagamentos) {
    }

    public record HistoricoResponse(StatusPedido statusAnterior, StatusPedido statusNovo, String motivo,
                                    String alteradoPor, LocalDateTime alteradoEm) {
    }

    /** Comprovante de compra exibido no app: contem o QR Code da retirada. */
    public record ComprovanteResponse(Long pedidoId, String numero, String codigoRetirada, String qrCodeBase64,
                                      String eventoTitulo, String local, LocalDateTime dataEvento,
                                      LocalDateTime retiradaInicio, LocalDateTime retiradaFim,
                                      String cliente, StatusPedido status, BigDecimal valorTotal,
                                      List<PedidoItemResponse> itens) {
    }

    /** Aviso enviado ao comprador (e-mail simulado). */
    public record NotificacaoResponse(Long id, Long pedidoId, String numeroPedido, String tipo, String destinatario,
                                      String assunto, String mensagem, LocalDateTime enviadaEm, String status) {
    }

    public record IniciarPagamentoRequest(@NotNull(message = "Informe o metodo (PIX, CARTAO ou DINHEIRO)") MetodoPagamento metodo) {
    }
}
