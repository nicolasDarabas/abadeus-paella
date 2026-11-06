package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Lote;
import br.com.abadeus.paellaapi.domain.entity.LoteProduto;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteProdutoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteProdutoResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.LoteResponse;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.exception.RegraNegocioException;
import br.com.abadeus.paellaapi.repository.LoteProdutoRepository;
import br.com.abadeus.paellaapi.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LoteService {

    private final LoteRepository loteRepository;
    private final LoteProdutoRepository loteProdutoRepository;
    private final EventoService eventoService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    public LoteService(LoteRepository loteRepository, LoteProdutoRepository loteProdutoRepository,
                       EventoService eventoService, ProdutoService produtoService, EstoqueService estoqueService) {
        this.loteRepository = loteRepository;
        this.loteProdutoRepository = loteProdutoRepository;
        this.eventoService = eventoService;
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
    }

    public List<LoteResponse> listarPorEvento(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        return loteRepository.findByEventoIdOrderByVendaInicioAsc(eventoId).stream().map(this::paraResponse).toList();
    }

    public LoteResponse buscar(Long id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public LoteResponse criar(Long eventoId, LoteRequest req) {
        Lote lote = new Lote();
        lote.setEvento(eventoService.buscarEntidade(eventoId));
        aplicar(lote, req);
        return paraResponse(loteRepository.save(lote));
    }

    @Transactional
    public LoteResponse atualizar(Long id, LoteRequest req) {
        Lote lote = buscarEntidade(id);
        aplicar(lote, req);
        return paraResponse(lote);
    }

    /** Adiciona o produto ao lote ou atualiza preco/quantidade se ja estiver nele. */
    @Transactional
    public LoteResponse definirProduto(Long loteId, LoteProdutoRequest req) {
        Lote lote = buscarEntidade(loteId);
        Produto produto = produtoService.buscarEntidade(req.produtoId());
        if (!produto.getEvento().getId().equals(lote.getEvento().getId())) {
            throw new RegraNegocioException("O produto pertence a outro evento.");
        }
        LoteProduto lp = loteProdutoRepository.findByLoteIdAndProdutoId(loteId, produto.getId())
                .orElseGet(LoteProduto::new);
        lp.setLote(lote);
        lp.setProduto(produto);
        lp.setPrecoLote(req.precoLote());
        lp.setQuantidade(req.quantidade());
        loteProdutoRepository.save(lp);
        return paraResponse(lote);
    }

    @Transactional
    public LoteResponse removerProduto(Long loteId, Long produtoId) {
        LoteProduto lp = loteProdutoRepository.findByLoteIdAndProdutoId(loteId, produtoId)
                .orElseThrow(() -> new NaoEncontradoException("Produto " + produtoId + " nao esta no lote " + loteId));
        if (estoqueService.vendidosNoLote(produtoId, loteId) > 0) {
            throw new RegraNegocioException("Ja existem pedidos neste lote para o produto. Desative o lote em vez de remover.");
        }
        loteProdutoRepository.delete(lp);
        return paraResponse(lp.getLote());
    }

    private Lote buscarEntidade(Long id) {
        return loteRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Lote", id));
    }

    private void aplicar(Lote lote, LoteRequest req) {
        if (!req.vendaFim().isAfter(req.vendaInicio())) {
            throw new RegraNegocioException("O fim das vendas do lote deve ser depois do inicio.");
        }
        lote.setNome(req.nome().trim());
        lote.setVendaInicio(req.vendaInicio());
        lote.setVendaFim(req.vendaFim());
        lote.setAtivo(req.ativo() == null || req.ativo());
    }

    private LoteResponse paraResponse(Lote lote) {
        List<LoteProdutoResponse> produtos = loteProdutoRepository.findByLoteId(lote.getId()).stream()
                .map(lp -> new LoteProdutoResponse(lp.getId(), lp.getProduto().getId(), lp.getProduto().getNome(),
                        lp.getPrecoLote(), lp.getQuantidade(),
                        estoqueService.vendidosNoLote(lp.getProduto().getId(), lote.getId())))
                .toList();
        return new LoteResponse(lote.getId(), lote.getEvento().getId(), lote.getNome(), lote.getVendaInicio(),
                lote.getVendaFim(), lote.isAtivo(), lote.vigenteEm(LocalDateTime.now()), produtos);
    }
}
