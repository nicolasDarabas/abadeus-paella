package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.PedidoDtos.NotificacaoResponse;
import br.com.abadeus.paellaapi.service.NotificacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Avisos recebidos pelo usuario logado (e-mails simulados). */
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/minhas")
    public List<NotificacaoResponse> minhas() {
        return notificacaoService.minhas();
    }
}
