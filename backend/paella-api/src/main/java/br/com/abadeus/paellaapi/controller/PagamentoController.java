package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.PedidoDtos.PagamentoResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoResponse;
import br.com.abadeus.paellaapi.service.PagamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    /**
     * SIMULACAO: faz o papel do banco aprovando o PIX/CARTAO.
     * Numa integracao real, isso viria do webhook do provedor de pagamento.
     */
    @PostMapping("/{id}/simular-aprovacao")
    public PedidoResponse simularAprovacao(@PathVariable Long id) {
        return pagamentoService.simularAprovacao(id);
    }

    /** Administrador confirma o pagamento (ex.: dinheiro no caixa). */
    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PedidoResponse confirmar(@PathVariable Long id) {
        return pagamentoService.confirmar(id);
    }

    @PostMapping("/{id}/recusar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PagamentoResponse recusar(@PathVariable Long id) {
        return pagamentoService.recusar(id);
    }
}
