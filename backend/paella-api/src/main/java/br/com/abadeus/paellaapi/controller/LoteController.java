package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.EventoDtos.LoteProdutoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteResponse;
import br.com.abadeus.paellaapi.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @GetMapping("/{id}")
    public LoteResponse buscar(@PathVariable Long id) {
        return loteService.buscar(id);
    }

    @PutMapping("/{id}")
    public LoteResponse atualizar(@PathVariable Long id, @Valid @RequestBody LoteRequest req) {
        return loteService.atualizar(id, req);
    }

    /** Coloca um produto no lote (ou atualiza preco/quantidade). */
    @PostMapping("/{id}/produtos")
    public LoteResponse definirProduto(@PathVariable Long id, @Valid @RequestBody LoteProdutoRequest req) {
        return loteService.definirProduto(id, req);
    }

    @DeleteMapping("/{id}/produtos/{produtoId}")
    public LoteResponse removerProduto(@PathVariable Long id, @PathVariable Long produtoId) {
        return loteService.removerProduto(id, produtoId);
    }
}
