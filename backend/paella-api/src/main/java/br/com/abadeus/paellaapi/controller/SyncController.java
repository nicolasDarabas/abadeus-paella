package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.RetiradaDtos.PendenteRetiradaResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.SyncResultado;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.SyncRetiradasRequest;
import br.com.abadeus.paellaapi.service.RetiradaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Modo contingencia: o app do administrador baixa os pedidos pagos para o banco local,
 * valida retiradas sem internet e depois envia tudo para o servidor.
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final RetiradaService retiradaService;

    public SyncController(RetiradaService retiradaService) {
        this.retiradaService = retiradaService;
    }

    @GetMapping("/eventos/{id}/retiradas-pendentes")
    public List<PendenteRetiradaResponse> pendentes(@PathVariable Long id) {
        return retiradaService.pendentes(id);
    }

    @PostMapping("/retiradas")
    public List<SyncResultado> sincronizar(@Valid @RequestBody SyncRetiradasRequest req) {
        return retiradaService.sincronizar(req.retiradas());
    }
}
