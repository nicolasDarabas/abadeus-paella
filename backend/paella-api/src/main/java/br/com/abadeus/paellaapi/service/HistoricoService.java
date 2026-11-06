package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Pedido;
import br.com.abadeus.paellaapi.domain.entity.PedidoStatusHistorico;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.dto.PedidoDtos.HistoricoResponse;
import br.com.abadeus.paellaapi.repository.PedidoStatusHistoricoRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Toda mudanca de status do pedido passa por aqui e fica registrada no historico. */
@Service
public class HistoricoService {

    private final PedidoStatusHistoricoRepository repository;

    public HistoricoService(PedidoStatusHistoricoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void alterarStatus(Pedido pedido, StatusPedido novo, String motivo) {
        StatusPedido anterior = pedido.getStatus();
        pedido.setStatus(novo);
        PedidoStatusHistorico h = new PedidoStatusHistorico();
        h.setPedido(pedido);
        h.setStatusAnterior(anterior);
        h.setStatusNovo(novo);
        h.setMotivo(motivo);
        h.setAlteradoPor(SecurityUtils.loginAtualOuSistema());
        h.setAlteradoEm(LocalDateTime.now());
        repository.save(h);
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> listar(Long pedidoId) {
        return repository.findByPedidoIdOrderByAlteradoEmAsc(pedidoId).stream()
                .map(h -> new HistoricoResponse(h.getStatusAnterior(), h.getStatusNovo(), h.getMotivo(),
                        h.getAlteradoPor(), h.getAlteradoEm()))
                .toList();
    }
}
