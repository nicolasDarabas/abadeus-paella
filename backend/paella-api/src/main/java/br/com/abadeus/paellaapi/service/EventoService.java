package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.enums.StatusEvento;
import br.com.abadeus.paellaapi.dto.EventoDtos.CardapioItemResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.CardapioResponse;
import br.com.abadeus.paellaapi.dto.EventoDtos.EventoRequest;
import br.com.abadeus.paellaapi.dto.EventoDtos.EventoResponse;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.exception.RegraNegocioException;
import br.com.abadeus.paellaapi.repository.EventoRepository;
import br.com.abadeus.paellaapi.repository.ProdutoRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import br.com.abadeus.paellaapi.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;

    public EventoService(EventoRepository eventoRepository, ProdutoRepository produtoRepository,
                         EstoqueService estoqueService) {
        this.eventoRepository = eventoRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueService = estoqueService;
    }

    /** Publico ve eventos publicados; administrador ve todos (inclusive rascunhos). */
    public List<EventoResponse> listar() {
        boolean admin = SecurityUtils.usuarioAtualOpcional().map(UsuarioAutenticado::isAdministrador).orElse(false);
        List<Evento> eventos = admin
                ? eventoRepository.findAllByOrderByDataEventoDesc()
                : eventoRepository.findByStatusInOrderByDataEventoAsc(
                        EnumSet.of(StatusEvento.VENDAS_ABERTAS, StatusEvento.VENDAS_ENCERRADAS, StatusEvento.FINALIZADO));
        return eventos.stream().map(EventoResponse::de).toList();
    }

    public EventoResponse buscar(Long id) {
        return EventoResponse.de(buscarEntidade(id));
    }

    public Evento buscarEntidade(Long id) {
        return eventoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Evento", id));
    }

    /** Cardapio com preco atual (lote vigente) e quantidade disponivel. */
    public CardapioResponse cardapio(Long eventoId) {
        Evento evento = buscarEntidade(eventoId);
        LocalDateTime agora = LocalDateTime.now();
        List<CardapioItemResponse> itens = new ArrayList<>();
        for (Produto p : produtoRepository.findByEventoIdAndAtivoTrueOrderByNomeAsc(eventoId)) {
            EstoqueService.PrecoVigente pv = estoqueService.precoVigente(p, agora);
            Integer disponivel = estoqueService.disponivel(p);
            Integer noLote = estoqueService.disponivelNoLote(pv.loteProduto());
            if (noLote != null) {
                disponivel = disponivel == null ? noLote : Math.min(disponivel, noLote);
            }
            boolean esgotado = disponivel != null && disponivel <= 0;
            itens.add(new CardapioItemResponse(p.getId(), p.getNome(), p.getDescricao(), pv.preco(),
                    pv.loteProduto() == null ? null : pv.loteProduto().getLote().getId(),
                    pv.loteProduto() == null ? null : pv.loteProduto().getLote().getNome(),
                    disponivel, esgotado));
        }
        return new CardapioResponse(EventoResponse.de(evento), evento.getStatus() == StatusEvento.VENDAS_ABERTAS, itens);
    }

    @Transactional
    public EventoResponse criar(EventoRequest req) {
        Evento e = new Evento();
        aplicar(e, req);
        e.setStatus(StatusEvento.RASCUNHO);
        return EventoResponse.de(eventoRepository.save(e));
    }

    @Transactional
    public EventoResponse atualizar(Long id, EventoRequest req) {
        Evento e = buscarEntidade(id);
        aplicar(e, req);
        return EventoResponse.de(e);
    }

    @Transactional
    public EventoResponse alterarStatus(Long id, StatusEvento status) {
        Evento e = buscarEntidade(id);
        if (status == StatusEvento.VENDAS_ABERTAS && produtoRepository.findByEventoIdAndAtivoTrueOrderByNomeAsc(id).isEmpty()) {
            throw new RegraNegocioException("Cadastre pelo menos um produto ativo antes de abrir as vendas.");
        }
        e.setStatus(status);
        return EventoResponse.de(e);
    }

    private void aplicar(Evento e, EventoRequest req) {
        if (req.retiradaInicio() != null && req.retiradaFim() != null && req.retiradaFim().isBefore(req.retiradaInicio())) {
            throw new RegraNegocioException("O fim da retirada deve ser depois do inicio.");
        }
        e.setTitulo(req.titulo().trim());
        e.setDescricao(req.descricao());
        e.setLocal(req.local().trim());
        e.setDataEvento(req.dataEvento());
        e.setRetiradaInicio(req.retiradaInicio());
        e.setRetiradaFim(req.retiradaFim());
        e.setContato(req.contato());
        e.setRegras(req.regras());
    }
}
