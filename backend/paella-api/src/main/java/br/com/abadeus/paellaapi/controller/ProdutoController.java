package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.dto.EventoDtos.ProdutoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.ProdutoResponse;
import br.com.abadeus.paellaapi.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
        return produtoService.atualizar(id, req);
    }

    /** Desativa o produto (nao some do historico de pedidos). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        produtoService.desativar(id);
    }
}
