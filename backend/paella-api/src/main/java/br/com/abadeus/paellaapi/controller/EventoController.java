package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.EventoDtos.CardapioResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.EventoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.EventoResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.ProdutoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.ProdutoResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.StatusEventoRequest;
import br.com.abadeus.paellaapi.service.EventoService;
import br.com.abadeus.paellaapi.service.LoteService;
import br.com.abadeus.paellaapi.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final ProdutoService produtoService;
    private final LoteService loteService;

    public EventoController(EventoService eventoService, ProdutoService produtoService, LoteService loteService) {
        this.eventoService = eventoService;
        this.produtoService = produtoService;
        this.loteService = loteService;
    }

    // ---------- Publico ----------

    @GetMapping
    public List<EventoResponse> listar() {
        return eventoService.listar();
    }

    @GetMapping("/{id}")
    public EventoResponse buscar(@PathVariable Long id) {
        return eventoService.buscar(id);
    }

    /** Cardapio com preco do lote vigente e estoque disponivel. */
    @GetMapping("/{id}/cardapio")
    public CardapioResponse cardapio(@PathVariable Long id) {
        return eventoService.cardapio(id);
    }

    @GetMapping("/{id}/produtos")
    public List<ProdutoResponse> produtos(@PathVariable Long id) {
        return produtoService.listarPorEvento(id);
    }

    @GetMapping("/{id}/lotes")
    public List<LoteResponse> lotes(@PathVariable Long id) {
        return loteService.listarPorEvento(id);
    }

    // ---------- Administrador ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EventoResponse criar(@Valid @RequestBody EventoRequest req) {
        return eventoService.criar(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EventoResponse atualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest req) {
        return eventoService.atualizar(id, req);
    }

    /** RASCUNHO -> VENDAS_ABERTAS -> VENDAS_ENCERRADAS -> FINALIZADO */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EventoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusEventoRequest req) {
        return eventoService.alterarStatus(id, req.status());
    }

    @PostMapping("/{id}/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProdutoResponse criarProduto(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
        return produtoService.criar(id, req);
    }

    @PostMapping("/{id}/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public LoteResponse criarLote(@PathVariable Long id, @Valid @RequestBody LoteRequest req) {
        return loteService.criar(id, req);
    }
}
