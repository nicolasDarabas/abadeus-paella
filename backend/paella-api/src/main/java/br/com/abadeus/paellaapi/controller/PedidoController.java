package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.PedidoDtos.CancelarRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.ComprovanteResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.CriarPedidoRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.HistoricoResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.IniciarPagamentoRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PagamentoResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoResponse;
import br.com.abadeus.paellaapi.service.PagamentoService;
import br.com.abadeus.paellaapi.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pedidos do usuario logado (comprador ve so os seus; administrador ve todos). */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PagamentoService pagamentoService;

    public PedidoController(PedidoService pedidoService, PagamentoService pagamentoService) {
        this.pedidoService = pedidoService;
        this.pagamentoService = pagamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody CriarPedidoRequest req) {
        return pedidoService.criar(req);
    }

    @GetMapping("/meus")
    public List<PedidoResponse> meus() {
        return pedidoService.meusPedidos();
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return pedidoService.buscar(id);
    }

    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) CancelarRequest req) {
        return pedidoService.cancelar(id, req == null ? null : req.motivo());
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> historico(@PathVariable Long id) {
        return pedidoService.historico(id);
    }

    /** Comprovante de compra com o QR Code em Base64 (para exibir no app). */
    @GetMapping("/{id}/comprovante")
    public ComprovanteResponse comprovante(@PathVariable Long id) {
        return pedidoService.comprovante(id);
    }

    /** Somente a imagem PNG do QR Code. */
    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrcode(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(pedidoService.qrCodePng(id));
    }

    // ---------- Pagamento ----------

    /** Inicia o pagamento: PIX, CARTAO ou DINHEIRO. */
    @PostMapping("/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse pagar(@PathVariable Long id, @Valid @RequestBody IniciarPagamentoRequest req) {
        return pagamentoService.iniciar(id, req.metodo());
    }

    @GetMapping("/{id}/pagamentos")
    public List<PagamentoResponse> pagamentos(@PathVariable Long id) {
        return pagamentoService.listarPorPedido(id);
    }
}
