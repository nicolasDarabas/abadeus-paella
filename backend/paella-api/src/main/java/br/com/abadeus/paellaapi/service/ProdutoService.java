package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.dto.EventoDtos.ProdutoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.ProdutoResponse;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EventoService eventoService;

    public ProdutoService(ProdutoRepository produtoRepository, EventoService eventoService) {
        this.produtoRepository = produtoRepository;
        this.eventoService = eventoService;
    }

    public List<ProdutoResponse> listarPorEvento(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        return produtoRepository.findByEventoIdOrderByNomeAsc(eventoId).stream().map(ProdutoResponse::de).toList();
    }

    public Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Produto", id));
    }

    @Transactional
    public ProdutoResponse criar(Long eventoId, ProdutoRequest req) {
        Evento evento = eventoService.buscarEntidade(eventoId);
        Produto p = new Produto();
        p.setEvento(evento);
        aplicar(p, req);
        return ProdutoResponse.de(produtoRepository.save(p));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest req) {
        Produto p = buscarEntidade(id);
        aplicar(p, req);
        return ProdutoResponse.de(p);
    }

    /** Nao apaga do banco (ha pedidos ligados): apenas desativa. */
    @Transactional
    public void desativar(Long id) {
        buscarEntidade(id).setAtivo(false);
    }

    private void aplicar(Produto p, ProdutoRequest req) {
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao());
        p.setPreco(req.preco());
        p.setLimiteEstoque(req.limiteEstoque());
        p.setAtivo(req.ativo() == null || req.ativo());
    }
}
